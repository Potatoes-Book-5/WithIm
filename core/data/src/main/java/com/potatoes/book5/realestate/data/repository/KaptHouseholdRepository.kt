package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.data.remote.KaptService
import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.ApartmentTrade
import com.potatoes.book5.realestate.domain.port.ApartmentHouseholdRepository
import java.io.IOException
import java.io.InterruptedIOException
import java.net.URLDecoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response

/** Joins independent RTMS/K-APT identifiers only after checking district, name and address. */
internal class KaptHouseholdRepository(
    private val service: KaptService,
    private val apiKey: String,
) : ApartmentHouseholdRepository {
    private val mutex = Mutex()
    // A bounded, process-local cache; never persist the provider's response or credentials.
    private val districts = LinkedHashMap<String, List<JSONObject>>()
    private val details = LinkedHashMap<String, JSONObject>()
    private val complexCodes = LinkedHashMap<String, String>()

    override suspend fun households(trade: ApartmentTrade): Outcome<Int?> = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (apiKey.isBlank()) return@withLock Outcome.Failure(DomainError.MissingApiKey)
            if (!trade.districtCode.matches(Regex("[0-9]{5}"))) {
                return@withLock Outcome.Failure(DomainError.Invalid("invalid-district-code"))
            }
            val name = trade.apartmentName?.normalized().orEmpty()
            val dong = trade.legalDong?.trim().orEmpty()
            if (name.isEmpty() || dong.isEmpty()) return@withLock Outcome.Success(null)
            try {
                val key = if ('%' in apiKey) URLDecoder.decode(apiKey.replace("+", "%2B"), "UTF-8") else apiKey
                val tradeKey = trade.apartmentId?.takeIf { it.isNotBlank() }?.let { "${trade.districtCode}:$it" }
                val code = tradeKey?.let { complexCodes[it] } ?: run {
                    val candidates = complexList(key, trade.districtCode).filter {
                        it.optString("kaptName").normalized() == name &&
                            it.optString("as3").trim() == dong &&
                            it.optString("bjdCode").startsWith(trade.districtCode)
                    }
                    // An ambiguous name must never select another phase's household count.
                    if (candidates.size != 1) return@withLock Outcome.Success(null)
                    candidates.single().getString("kaptCode")
                }
                val info = details[code] ?: body(service.basicInfo(key, code)).getJSONObject("item").also {
                    if (it.optString("kaptCode") != code) throw InvalidKaptResponse()
                    details.putBounded(code, it, 256)
                }
                if (!info.optString("bjdCode").startsWith(trade.districtCode) ||
                    info.optString("kaptName").normalized() != name || !addressMatches(info, trade)) {
                    return@withLock Outcome.Success(null)
                }
                if (tradeKey != null) complexCodes.putBounded(tradeKey, code, 256)
                val count = info.optString("kaptdaCnt").toBigDecimalOrNull()?.let {
                    try { it.intValueExact() } catch (_: ArithmeticException) { null }
                }?.takeIf { it > 0 }
                Outcome.Success(count)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: HttpException) {
                error.response()?.errorBody()?.close()
                Outcome.Failure(DomainError.Http(error.code()))
            } catch (_: KaptProviderRejected) {
                Outcome.Failure(DomainError.ProviderRejected)
            } catch (_: InvalidKaptResponse) {
                Outcome.Failure(DomainError.InvalidResponse)
            } catch (_: JSONException) {
                Outcome.Failure(DomainError.InvalidResponse)
            } catch (_: IllegalArgumentException) {
                Outcome.Failure(DomainError.Invalid("invalid-api-key-encoding"))
            } catch (_: InterruptedIOException) {
                Outcome.Failure(DomainError.Timeout)
            } catch (_: IOException) {
                Outcome.Failure(DomainError.Offline)
            }
        }
    }

    private suspend fun complexList(key: String, district: String): List<JSONObject> {
        districts[district]?.let { return it }
        val result = mutableListOf<JSONObject>()
        var total: Int? = null
        var page = 1
        while (true) {
            val response = body(service.complexes(key, district, page, PAGE_SIZE))
            val count = response.getString("totalCount").toIntOrNull() ?: throw InvalidKaptResponse()
            if (count !in 0..MAX_COMPLEXES || (total != null && total != count) ||
                response.getString("pageNo").toIntOrNull() != page) throw InvalidKaptResponse()
            total = count
            val items = response.optJSONArray("items") ?: if (count == 0) JSONArray() else throw InvalidKaptResponse()
            if (items.length() > PAGE_SIZE) throw InvalidKaptResponse()
            for (index in 0 until items.length()) {
                val item = items.getJSONObject(index)
                if (item.optString("kaptCode").isBlank() || !item.optString("bjdCode").startsWith(district)) {
                    throw InvalidKaptResponse()
                }
                result += item
            }
            if (result.size == count) break
            if (items.length() == 0 || result.size > count || page * PAGE_SIZE >= MAX_COMPLEXES) throw InvalidKaptResponse()
            page++
        }
        if (result.map { it.getString("kaptCode") }.distinct().size != result.size) throw InvalidKaptResponse()
        return result.toList().also { districts.putBounded(district, it, 8) }
    }

    private fun body(response: Response<ResponseBody>): JSONObject {
        if (!response.isSuccessful) throw HttpException(response)
        val raw = response.body() ?: throw InvalidKaptResponse()
        return raw.use {
            if (it.source().request(1_048_577)) throw InvalidKaptResponse()
            val json = JSONObject(it.string()).getJSONObject("response")
            if (json.getJSONObject("header").optString("resultCode") != "00") throw KaptProviderRejected()
            json.getJSONObject("body")
        }
    }

    private fun addressMatches(info: JSONObject, trade: ApartmentTrade): Boolean {
        val lot = trade.lotNumber?.trim()?.takeIf { it.isNotEmpty() }
        val legal = lot?.let {
            Regex("(?:^|\\s)" + Regex.escape(trade.legalDong!!.trim()) + "\\s+" + Regex.escape(it) + "(?:\\s|$)")
                .containsMatchIn(info.optString("kaptAddr"))
        } ?: false
        val road = trade.roadAddress?.addressLine?.let {
            Regex("(?:^|\\s)" + Regex.escape(it) + "(?:\\s|\\(|$)")
                .containsMatchIn(info.optString("doroJuso"))
        } ?: false
        return legal || road
    }

    private fun String.normalized() = replace(Regex("\\s+"), "").removeSuffix("아파트")

    private fun <T> LinkedHashMap<String, T>.putBounded(key: String, value: T, limit: Int) {
        if (size >= limit && !containsKey(key)) remove(keys.first())
        put(key, value)
    }

    private class InvalidKaptResponse : RuntimeException()
    private class KaptProviderRejected : RuntimeException()

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_COMPLEXES = 10_000 // Application bound, not a provider quota.
    }
}
