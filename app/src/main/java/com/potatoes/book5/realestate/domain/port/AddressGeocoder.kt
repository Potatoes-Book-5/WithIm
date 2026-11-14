package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.AddressKind
import com.potatoes.book5.realestate.domain.model.GeoCoordinates

interface AddressGeocoder {
    suspend fun geocode(address: String, kind: AddressKind): Outcome<GeoCoordinates>
}
