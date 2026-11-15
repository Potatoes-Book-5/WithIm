package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.model.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate

internal data class PreparedMarketBatch(
    val candidates: List<PreparedScrapCandidate>,
    val prices: PreparedDistrictPrices,
)

/** Runs on the collector side, only after complete region/month responses have arrived. */
internal fun prepareMarketBatch(
    districtCode: String,
    districtName: String,
    from: LocalDate,
    to: LocalDate,
    sourceAsOf: Instant,
    trades: List<Pair<String?, ApartmentTrade>>,
): PreparedMarketBatch {
    val valid = trades.filter { (_, t) ->
        t.districtCode == districtCode && !t.hasCancellationNotice &&
            t.contractDate in from..to && t.priceManwon in 1..(Long.MAX_VALUE / 10_000) &&
            t.exclusiveAreaSquareMeters?.let { it > BigDecimal.ZERO } == true
    }
    val candidates = valid.filter { it.first != null && it.second.apartmentName?.isNotBlank() == true }.groupBy { it.first!! }.mapNotNull { (id, rows) ->
        // Latest transaction is the scrap baseline; ranking's maximum must not replace it.
        val t = rows.maxWithOrNull(compareBy<Pair<String?, ApartmentTrade>> { it.second.contractDate }
            .thenBy { it.second.priceManwon })?.second ?: return@mapNotNull null
        val area = t.exclusiveAreaSquareMeters!!.divide(BigDecimal("3.305785"), 0, RoundingMode.HALF_UP)
        if (area > BigDecimal(Int.MAX_VALUE)) return@mapNotNull null
        PreparedScrapCandidate(id, t.apartmentName!!, t.legalDong ?: "동 정보 없음", area.toInt().coerceAtLeast(1),
            PriceRecord(t.priceManwon, t.contractDate), t.builtYear, sourceAsOf, districtCode)
    }
    val samples = valid.filter { (_, t) ->
        t.exclusiveAreaSquareMeters!!.let { area -> area >= BigDecimal(80) && area < BigDecimal(90) }
    }
    val sum = samples.fold(BigInteger.ZERO) { acc, row -> acc + BigInteger.valueOf(row.second.priceManwon) * BigInteger.valueOf(10_000) }
    val highest = samples.maxWithOrNull(compareBy<Pair<String?, ApartmentTrade>> { it.second.priceManwon }
        .thenBy { it.second.contractDate }.thenBy { it.first })
    return PreparedMarketBatch(candidates, PreparedDistrictPrices(districtCode, districtName, from, to, samples.size,
        if (samples.isEmpty()) null else BigDecimal(sum).divide(BigDecimal(samples.size), 0, RoundingMode.HALF_UP).longValueExact(),
        highest?.second?.priceManwon?.times(10_000), highest?.first, sourceAsOf,
        highestComplexName = highest?.second?.apartmentName?.takeIf { it.isNotBlank() }))
}
