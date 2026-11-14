package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.ApartmentTrade
import com.potatoes.book5.realestate.domain.model.GeoCoordinates
import com.potatoes.book5.realestate.domain.model.LocatedApartmentTrade
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Highest individual reported transactions within 10 km of the temporary Samsung reference. */
class GetSamsungTradeRankingUseCase(
    private val getFeed: GetYeongtongTradeFeedUseCase,
    private val geocodeTrade: GeocodeYeongtongTradeUseCase,
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now(ZoneId.of("Asia/Seoul")), limit: Int = 3): Outcome<List<LocatedApartmentTrade>> {
        if (limit !in 1..3) return Outcome.Failure(DomainError.Invalid("invalid-result-limit"))

        val fromDate = today.minusMonths(1)
        val trades = mutableListOf<ApartmentTrade>()
        var month = YearMonth.from(fromDate)
        val lastMonth = YearMonth.from(today)
        while (month <= lastMonth) {
            val feed = when (val result = getFeed(month)) {
                is Outcome.Failure -> return result
                is Outcome.Success -> result.value
            }
            trades += feed.trades.filter { it.contractDate in fromDate..today }
            month = month.plusMonths(1)
        }
        val ranked = trades.asSequence()
            .filterNot { it.hasCancellationNotice }
            .sortedWith(compareByDescending<ApartmentTrade> { it.priceManwon }
                .thenByDescending { it.contractDate })

        // Vworld coordinates may be reused only while this single request is running.
        // A null value records an invalid address supplied by one transaction.
        val pointsByAddress = mutableMapOf<String, GeoCoordinates?>()
        val result = mutableListOf<LocatedApartmentTrade>()
        for (trade in ranked) {
            val addressKey = trade.addressCacheKey() ?: continue
            val point = if (pointsByAddress.containsKey(addressKey)) {
                pointsByAddress[addressKey]
            } else {
                when (val geocoded = geocodeTrade(trade)) {
                    is Outcome.Success -> geocoded.value.also { pointsByAddress[addressKey] = it }
                    is Outcome.Failure -> when (val error = geocoded.error) {
                        is DomainError.Invalid -> if (error.reason == "trade-address-unavailable" ||
                            error.reason == "invalid-address") {
                            pointsByAddress[addressKey] = null
                            null
                        } else return geocoded
                        else -> return geocoded
                    }
                }
            } ?: continue

            val distance = greatCircleKilometers(GetSamsungReferenceTradesUseCase.REFERENCE, point)
            if (distance > GetSamsungReferenceTradesUseCase.RADIUS_KM) continue
            result += LocatedApartmentTrade(trade, point, distance, withinTenKm = true)
            if (result.size == limit) break
        }
        return Outcome.Success(result)
    }
}

private fun ApartmentTrade.addressCacheKey(): String? = roadAddress?.let { "road:${it.addressLine}" }
    ?: listOfNotNull(legalDong, lotNumber).takeIf { it.size == 2 }
        ?.joinToString(" ", prefix = "parcel:")
