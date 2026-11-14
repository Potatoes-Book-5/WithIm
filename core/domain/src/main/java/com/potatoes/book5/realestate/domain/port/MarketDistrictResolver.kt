package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.GeoPoint

/** `name` is Vworld's full_nm (for example, "경기도 수원시 영통구") for address geocoding. */
data class MarketDistrict(val code: String, val name: String)

/** Finds every market district whose boundary intersects a radius' bounding box. */
interface MarketDistrictResolver {
    suspend fun districts(center: GeoPoint, radiusKm: Double): Outcome<List<MarketDistrict>>
    /** Exactly one containing district. Ambiguous boundaries and missing data are failures. */
    suspend fun districtAt(point: GeoPoint): Outcome<MarketDistrict>
}
