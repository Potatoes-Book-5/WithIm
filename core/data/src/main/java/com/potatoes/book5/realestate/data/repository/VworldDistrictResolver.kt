package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.data.remote.VworldDistrictService
import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.GeoPoint
import com.potatoes.book5.realestate.domain.port.MarketDistrict
import com.potatoes.book5.realestate.domain.port.MarketDistrictResolver
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.math.cos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.HttpException

/** Vworld Data API 2.0, LT_C_ADSIGG_INFO. No disk cache or request logging. */
internal class VworldDistrictResolver(
    private val service: VworldDistrictService,
    private val apiKey: String,
) : MarketDistrictResolver {
    override suspend fun districts(center: GeoPoint, radiusKm: Double): Outcome<List<MarketDistrict>> {
        if (!center.latitude.isFinite() || !center.longitude.isFinite() ||
            center.latitude !in -90.0..90.0 || center.longitude !in -180.0..180.0 ||
            !radiusKm.isFinite() || radiusKm <= 0.0 || radiusKm > MAX_APP_RADIUS_KM
        ) return Outcome.Failure(DomainError.Invalid("invalid-district-query"))
        if (apiKey.isBlank()) return Outcome.Failure(DomainError.MissingApiKey)
        val box = boundingBox(center, radiusKm) ?: return Outcome.Failure(DomainError.Invalid("invalid-district-box"))
        return withContext(Dispatchers.IO) {
            val districts = linkedMapOf<String, MarketDistrict>()
            var expectedTotal: Int? = null
            var page = 1
            while (true) {
                val result = when (val response = fetchPage(box, page)) {
                    is Outcome.Failure -> return@withContext response
                    is Outcome.Success -> response.value
                }
                if (expectedTotal != null && expectedTotal != result.total) {
                    return@withContext Outcome.Failure(DomainError.InvalidResponse)
                }
                expectedTotal = result.total
                result.districts.forEach { district ->
                    val previous = districts.putIfAbsent(district.code, district)
                    if (previous != null && previous != district) {
                        return@withContext Outcome.Failure(DomainError.InvalidResponse)
                    }
                }
                if (page >= result.totalPages) break
                if (page >= MAX_PAGES) return@withContext Outcome.Failure(DomainError.InvalidResponse)
                page++
            }
            if (districts.size != expectedTotal) return@withContext Outcome.Failure(DomainError.InvalidResponse)
            Outcome.Success(districts.values.sortedBy { it.code })
        }
    }

    private suspend fun fetchPage(box: String, page: Int): Outcome<DistrictPage> {
        repeat(2) { attempt ->
            currentCoroutineContext().ensureActive()
            try {
                val response = service.getDistricts(apiKey, box, page, PAGE_SIZE)
                if (!response.isSuccessful) throw HttpException(response)
                val body = response.body() ?: return Outcome.Failure(DomainError.InvalidResponse)
                val parsed = body.use { parsePage(it.string(), page) }
                return Outcome.Success(parsed)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: HttpException) {
                error.response()?.errorBody()?.close()
                return Outcome.Failure(DomainError.Http(error.code()))
            } catch (_: ProviderRejected) {
                return Outcome.Failure(DomainError.ProviderRejected)
            } catch (_: InvalidDistrictResponse) {
                return Outcome.Failure(DomainError.InvalidResponse)
            } catch (_: JSONException) {
                return Outcome.Failure(DomainError.InvalidResponse)
            } catch (_: InterruptedIOException) {
                currentCoroutineContext().ensureActive()
                if (attempt == 1) return Outcome.Failure(DomainError.Timeout)
            } catch (_: IOException) {
                currentCoroutineContext().ensureActive()
                if (attempt == 1) return Outcome.Failure(DomainError.Offline)
            }
            delay(300)
        }
        error("Unreachable")
    }

    private fun parsePage(raw: String, requestedPage: Int): DistrictPage {
        if (raw.length > MAX_RESPONSE_CHARS) throw InvalidDistrictResponse()
        val root = JSONObject(raw).getJSONObject("response")
        if (root.optString("status") != "OK") throw ProviderRejected()
        val record = root.getJSONObject("record")
        val page = root.getJSONObject("page")
        val total = record.getString("total").toIntOrNull() ?: throw InvalidDistrictResponse()
        val current = record.getString("current").toIntOrNull() ?: throw InvalidDistrictResponse()
        val pageNumber = page.getString("current").toIntOrNull() ?: throw InvalidDistrictResponse()
        val pageSize = page.getString("size").toIntOrNull() ?: throw InvalidDistrictResponse()
        val totalPages = page.getString("total").toIntOrNull() ?: throw InvalidDistrictResponse()
        val features = root.getJSONObject("result").getJSONObject("featureCollection").getJSONArray("features")
        if (total < 0 || total > PAGE_SIZE * MAX_PAGES || pageNumber != requestedPage ||
            pageSize != PAGE_SIZE || totalPages != maxOf(1, (total + PAGE_SIZE - 1) / PAGE_SIZE) ||
            current != features.length() || current > PAGE_SIZE ||
            (requestedPage - 1L) * PAGE_SIZE + current > total ||
            (current == 0 && (requestedPage - 1L) * PAGE_SIZE < total)
        ) throw InvalidDistrictResponse()
        val districts = features.toDistricts()
        return DistrictPage(total, totalPages, districts)
    }

    private fun JSONArray.toDistricts(): List<MarketDistrict> = buildList {
        for (index in 0 until this@toDistricts.length()) {
            val properties = this@toDistricts.getJSONObject(index).getJSONObject("properties")
            val code = properties.getString("sig_cd").trim()
            val name = properties.getString("full_nm").trim()
            if (!code.matches(Regex("[0-9]{5}")) || name.isBlank() ||
                properties.getString("sig_kor_nm").isBlank()) throw InvalidDistrictResponse()
            add(MarketDistrict(code, name))
        }
    }

    /** Conservative rectangle around the 10 km circle; final trade distance is filtered separately. */
    private fun boundingBox(center: GeoPoint, radiusKm: Double): String? {
        val latitudeRadians = Math.toRadians(center.latitude)
        val longitudeScale = cos(latitudeRadians)
        if (longitudeScale <= 0.0) return null
        val latDelta = Math.toDegrees(radiusKm / EARTH_RADIUS_KM)
        val lonDelta = Math.toDegrees(radiusKm / (EARTH_RADIUS_KM * longitudeScale))
        val south = center.latitude - latDelta
        val north = center.latitude + latDelta
        val west = center.longitude - lonDelta
        val east = center.longitude + lonDelta
        if (south < -90.0 || north > 90.0 || west < -180.0 || east > 180.0) return null
        return "BOX($west,$south,$east,$north)"
    }

    private data class DistrictPage(val total: Int, val totalPages: Int, val districts: List<MarketDistrict>)
    private class InvalidDistrictResponse : RuntimeException()
    private class ProviderRejected : RuntimeException()

    private companion object {
        const val EARTH_RADIUS_KM = 6371.0088
        const val MAX_APP_RADIUS_KM = 100.0
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 50
        const val MAX_RESPONSE_CHARS = 500_000
    }
}
