package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.ApartmentTrade

/** Official complex household count. Null means no unambiguous address/name match or no count. */
interface ApartmentHouseholdRepository {
    suspend fun households(trade: ApartmentTrade): Outcome<Int?>
}
