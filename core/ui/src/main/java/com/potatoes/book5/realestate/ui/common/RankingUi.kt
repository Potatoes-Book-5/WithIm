package com.potatoes.book5.realestate.ui.common

import com.potatoes.book5.realestate.domain.model.GeoPoint
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import java.time.LocalDate

/** 홈과 주행 화면이 함께 쓰는 순위 항목 표시 모델. */
data class RankItemUi(
    val id: String,
    val rank: Int,
    val name: String,
    val priceManwon: Long,
    val areaPyeong: Int,
    val dealDate: LocalDate,
    val distanceKm: Double,
    val bearingDeg: Double,
    val location: GeoPoint? = null,
    val directionKnown: Boolean = true,
) {
    val isAhead: Boolean get() = directionKnown && bearingDeg in -90.0..90.0
}

/** 지도에 찍을 점(관심 매물, 그 밖의 단지). */
data class MapPointUi(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val bearingDeg: Double,
    val location: GeoPoint? = null,
)

fun NearbyApartment.toRankItem(rank: Int) = RankItemUi(
    id = apartment.id,
    rank = rank,
    name = apartment.name,
    priceManwon = apartment.priceManwon,
    areaPyeong = apartment.areaPyeong,
    dealDate = apartment.dealDate,
    distanceKm = distanceKm,
    bearingDeg = bearingDeg,
    location = apartment.location,
    directionKnown = directionKnown,
)

fun NearbyApartment.toMapPoint() = MapPointUi(apartment.id, apartment.name, distanceKm, bearingDeg, apartment.location)
