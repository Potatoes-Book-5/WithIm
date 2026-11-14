package com.potatoes.book5.realestate.domain.model

import java.time.Instant
import java.time.LocalDate

/** Minimum source data needed to save a scrap; no coordinates or full-detail fetch. */
data class PreparedScrapCandidate(
    val apartmentId: String,
    val name: String,
    val neighborhood: String,
    val areaPyeong: Int,
    val latest: PriceRecord,
    val builtYear: Int?,
    val sourceAsOf: Instant,
    val districtCode: String,
    val households: Int? = null,
)

/** Full-district raw transaction aggregation, deliberately distinct from 3km ranking. */
data class PreparedDistrictPrices(
    val districtCode: String,
    val districtName: String,
    val periodStart: LocalDate,
    val periodEndInclusive: LocalDate,
    val sampleCount: Int,
    val averagePriceKrw: Long?,
    val highestPriceKrw: Long?,
    val highestComplexId: String?,
    val sourceAsOf: Instant,
    val areaMinM2: Int = 80,
    val areaMaxExclusiveM2: Int = 90,
    val scope: String = "DISTRICT",
)
