package com.potatoes.book5.realestate.domain.model

import java.math.BigDecimal
import java.time.LocalDate

/** A reported transaction; price is in 만원, area is exclusive area in square meters. */
data class ApartmentTrade(
    val apartmentId: String?,
    val apartmentName: String?,
    val districtCode: String,
    val legalDong: String?,
    val lotNumber: String?,
    val priceManwon: Long,
    val exclusiveAreaSquareMeters: BigDecimal?,
    val contractDate: LocalDate,
    val floor: Int?,
    val builtYear: Int?,
    val roadAddress: TradeRoadAddress?,
    val cancellationType: String?,
    val cancellationDate: String?,
) {
    /** A notice is present; the provider flag's value meanings are not guessed. */
    val hasCancellationNotice: Boolean get() = cancellationType != null || cancellationDate != null
}

data class TradeRoadAddress(val roadName: String, val mainNumber: Int, val subNumber: Int?) {
    val addressLine: String get() = "$roadName $mainNumber" + (subNumber?.let { "-$it" } ?: "")
}

data class TradePage(
    val trades: List<ApartmentTrade>,
    val pageNumber: Int,
    val pageSize: Int,
    val totalCount: Int,
) {
    val nextPage: Int? get() =
        if (pageNumber.toLong() * pageSize < totalCount) pageNumber + 1 else null
}
