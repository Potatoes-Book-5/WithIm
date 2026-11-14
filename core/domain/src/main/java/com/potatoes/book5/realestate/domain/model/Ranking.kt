package com.potatoes.book5.realestate.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import com.potatoes.book5.realestate.domain.error.DomainError

/** 아파트 단지와 대표 실거래. 가격은 만원 단위(예: 24억 5,000만 원 = 245000). */
data class Apartment(
    val id: String,
    val name: String,
    val priceManwon: Long,
    val areaPyeong: Int,
    val dealDate: LocalDate,
    /** 단지 좌표. 데이터 출처(T-005)가 좌표를 주지 않으면 null */
    val location: GeoPoint? = null,
)

/** WGS84 위경도. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * 차량 위치와 진행 방향.
 * @param headingDeg 북쪽 기준 시계 방향 진행 방위(0~360)
 */
data class VehiclePose(val location: GeoPoint, val headingDeg: Double? = null)

/**
 * 차량 기준 주변 단지.
 * @param bearingDeg 진행 방향 기준 상대 방위(-180~180, 0이 정면, 양수가 오른쪽)
 */
data class NearbyApartment(
    val apartment: Apartment,
    val distanceKm: Double,
    val bearingDeg: Double,
    val directionKnown: Boolean = true,
) {
    val isAhead: Boolean get() = bearingDeg in -90.0..90.0
}

/** 반경 안 순위 스냅샷(F-002, F-003). */
data class RankingSnapshot(
    val neighborhoodName: String,
    val radiusKm: Int,
    /** 가격 순 1~3위 */
    val top: List<NearbyApartment>,
    /** 관심 조건과 일치하는 주변 단지(F-005) */
    val interestMatches: List<NearbyApartment>,
    /** 지도에 점으로만 표시하는 그 밖의 단지 */
    val others: List<NearbyApartment>,
    /** 실거래 데이터 기준 시각 */
    val dataAsOf: LocalDateTime,
    /** 확인된 차량 위치. 임시 조회 기준에서는 null이며 차량 마커와 근접 알림에 쓰지 않는다. */
    val vehicle: VehiclePose? = null,
    /** 지도와 거리 계산의 기준점. 임시 기준일 때도 차량 위치와 구분해 전달한다. */
    val mapCenter: VehiclePose? = null,
    val loading: Boolean = false,
    val error: DomainError? = null,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val referenceLabel: String? = null,
    /** Confirmed district visit. Returning to a district starts a different round. */
    val round: MarketRound? = null,
) {
    val leader: NearbyApartment? get() = top.firstOrNull()
}

/** 대장 아파트 교체 이벤트(F-003 규칙 4). */
data class LeaderChange(
    val previous: Apartment,
    val current: NearbyApartment,
    val at: LocalDateTime,
    val kind: LeaderEventKind = LeaderEventKind.REPLACED,
    val districtName: String? = null,
)

data class MarketRound(val id: String, val districtCode: String, val districtName: String)

enum class LeaderEventKind { DISCOVERED, REPLACED }

enum class DrivingState { PARKED, DRIVING }

/** 스크랩 요약(F-008). */
data class ScrapSummary(
    val totalCount: Int,
    val changedCount: Int,
    /** 변동 단지 중 첫 번째 이름과 변동 내용. 변동이 없으면 null */
    val firstChange: ScrapChange?,
)

data class ScrapChange(val apartmentName: String, val kind: ScrapChangeKind)

enum class ScrapChangeKind { NEW_DEAL, PRICE_UP, PRICE_DOWN, RANK_CHANGE }
