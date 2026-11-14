package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.AddressKind
import com.potatoes.book5.realestate.domain.model.ApartmentTrade
import com.potatoes.book5.realestate.domain.model.GeoCoordinates
import com.potatoes.book5.realestate.domain.port.AddressGeocoder

/** Geocodes one selected transaction. No vehicle-relative distance or bearing is implied. */
class GeocodeYeongtongTradeUseCase(private val geocoder: AddressGeocoder) {
    suspend operator fun invoke(trade: ApartmentTrade): Outcome<GeoCoordinates> {
        if (trade.districtCode != GetYeongtongTradeFeedUseCase.DISTRICT_CODE) {
            return Outcome.Failure(DomainError.Invalid("wrong-district"))
        }
        val prefix = "경기도 ${GetYeongtongTradeFeedUseCase.DISTRICT_NAME}"
        val road = trade.roadAddress
        if (road != null) return geocoder.geocode("$prefix ${road.addressLine}", AddressKind.ROAD)
        val dong = trade.legalDong
        val lot = trade.lotNumber
        if (!dong.isNullOrBlank() && !lot.isNullOrBlank()) {
            return geocoder.geocode("$prefix $dong $lot", AddressKind.PARCEL)
        }
        return Outcome.Failure(DomainError.Invalid("trade-address-unavailable"))
    }
}
