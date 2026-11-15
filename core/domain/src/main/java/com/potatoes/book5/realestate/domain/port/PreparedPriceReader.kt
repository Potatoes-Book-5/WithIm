package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.PreparedDistrictPrices
import com.potatoes.book5.realestate.domain.model.appaction.ResolvedArea

/** Immediate immutable reads only. Implementations must not fetch or wait for the collector. */
interface PreparedPriceReader {
    fun readDistrictPrices(districtCode: String): Outcome<PreparedDistrictPrices>
    fun knownDistricts(): List<ResolvedArea>
}
