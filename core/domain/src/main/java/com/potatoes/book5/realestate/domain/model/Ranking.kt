package com.potatoes.book5.realestate.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/** 아파트 단지와 대표 실거래. 가격은 만원 단위(예: 24억 5,000만 원 = 245000). */
data class Apartment(
    val id: String,
    val name: String,
    val priceManwon: Long,
    val areaPyeong: Int,
    val dealDate: LocalDate,
)

/**
 * 차량 기준 주변 단지.
 * @param bearingDeg 진행 방향 기준 상대 방위(-180~180, 0이 정면, 양수가 오른쪽)
 */
data class NearbyApartment(
    val apartment: Apartment,
    val distanceKm: Double,
    val bearingDeg: Double,
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
) {
    val leader: NearbyApartment? get() = top.firstOrNull()
}

/** 대장 아파트 교체 이벤트(F-003 규칙 4). */
data class LeaderChange(
    val previous: Apartment,
    val current: NearbyApartment,
    val at: LocalDateTime,
)

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
