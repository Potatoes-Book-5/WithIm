package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.GeoCoordinates
import com.potatoes.book5.realestate.domain.model.LocatedApartmentTrade
import java.time.YearMonth
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Preview data until the vehicle location provider is connected. Not a complex-price ranking. */
class GetSamsungReferenceTradesUseCase(
    private val getFeed: GetYeongtongTradeFeedUseCase,
    private val geocodeTrade: GeocodeYeongtongTradeUseCase,
) {
    suspend operator fun invoke(month: YearMonth, limit: Int = 3): Outcome<List<LocatedApartmentTrade>> {
        if (limit !in 1..10) return Outcome.Failure(DomainError.Invalid("invalid-result-limit"))
        val feed = when (val result = getFeed(month)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value
        }
        val result = mutableListOf<LocatedApartmentTrade>()
        val coordinatesByAddress = mutableMapOf<String, GeoCoordinates>()
        for (trade in feed.trades) {
            val address = trade.roadAddress?.addressLine ?: listOfNotNull(trade.legalDong, trade.lotNumber)
                .takeIf { it.size == 2 }?.joinToString(" ") ?: continue
            val point = coordinatesByAddress[address] ?: when (val geocoded = geocodeTrade(trade)) {
                is Outcome.Failure -> return geocoded
                is Outcome.Success -> geocoded.value.also { coordinatesByAddress[address] = it }
            }
            val distance = greatCircleKilometers(REFERENCE, point)
            result += LocatedApartmentTrade(trade, point, distance, distance <= RADIUS_KM)
            if (result.size == limit) break
        }
        return Outcome.Success(result)
    }

    companion object {
        const val REFERENCE_NAME = "삼성전자 수원 디지털시티"
        const val REFERENCE_ADDRESS = "경기도 수원시 영통구 삼성로 129"
        val REFERENCE = GeoCoordinates(longitude = 127.0454691833884, latitude = 37.253841010127545)
        const val RADIUS_KM = 10.0
    }
}

/** Great-circle distance for EPSG:4326 points; result is kilometers. */
internal fun greatCircleKilometers(a: GeoCoordinates, b: GeoCoordinates): Double {
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val latA = Math.toRadians(a.latitude)
    val latB = Math.toRadians(b.latitude)
    val haversine = (sin(dLat / 2).pow(2.0) + cos(latA) * cos(latB) * sin(dLon / 2).pow(2.0))
        .coerceIn(0.0, 1.0)
    return 2 * 6371.0088 * asin(min(1.0, sqrt(haversine)))
}
