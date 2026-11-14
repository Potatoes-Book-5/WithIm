package com.potatoes.book5.realestate.domain.model

/** EPSG:4326. Longitude is x and latitude is y in Vworld's response. */
data class GeoCoordinates(val longitude: Double, val latitude: Double)

enum class AddressKind { ROAD, PARCEL }

/** Raw reported transactions for a fixed district and contract month, not a nearby ranking. */
data class RegionalTradeFeed(
    val districtCode: String,
    val districtName: String,
    val contractMonth: java.time.YearMonth,
    val trades: List<ApartmentTrade>,
)

/** A transaction with its geocoded location and distance from a named fixed reference point. */
data class LocatedApartmentTrade(
    val trade: ApartmentTrade,
    val coordinates: GeoCoordinates,
    val distanceKm: Double,
    val withinTenKm: Boolean,
)
