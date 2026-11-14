package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.data.mapper.toDomain
import com.potatoes.book5.realestate.data.remote.InvalidTradeResponse
import com.potatoes.book5.realestate.data.remote.MolitService
import com.potatoes.book5.realestate.data.remote.TradeXmlParser
import com.potatoes.book5.realestate.data.remote.tradeRemoteRequest
import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.TradePage
import com.potatoes.book5.realestate.domain.port.ApartmentTradeRepository
import java.net.URLDecoder
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

internal class RemoteApartmentTradeRepository(
    private val service: MolitService,
    private val apiKey: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ApartmentTradeRepository {
    override suspend fun getTrades(
        districtCode: String,
        contractMonth: YearMonth,
        pageNumber: Int,
        pageSize: Int,
    ): Outcome<TradePage> {
        // 1000 is an application-side bound, not an inferred provider limit.
        if (!districtCode.matches(Regex("[0-9]{5}")) || contractMonth.year !in 1..9999 ||
            pageNumber < 1 || pageSize !in 1..1000
        ) return Outcome.Failure(DomainError.Invalid("invalid-trade-query"))
        if (apiKey.isBlank()) return Outcome.Failure(DomainError.MissingApiKey)
        val key = try {
            // URLDecoder treats '+' as a space. Escape literal plus before decoding percent-encoded keys.
            if ('%' in apiKey) URLDecoder.decode(apiKey.replace("+", "%2B"), "UTF-8") else apiKey
        } catch (_: IllegalArgumentException) {
            return Outcome.Failure(DomainError.Invalid("invalid-api-key-encoding"))
        }
        return withContext(io) {
            tradeRemoteRequest {
                val response = service.getTrades(key, districtCode,
                    contractMonth.format(DateTimeFormatter.ofPattern("uuuuMM")), pageNumber, pageSize)
                if (!response.isSuccessful) throw HttpException(response)
                val body = response.body() ?: throw InvalidTradeResponse()
                body.use {
                    TradeXmlParser.parse(it.byteStream(), pageSize)
                        .toDomain(districtCode, contractMonth, pageNumber, pageSize)
                }
            }
        }
    }
}
