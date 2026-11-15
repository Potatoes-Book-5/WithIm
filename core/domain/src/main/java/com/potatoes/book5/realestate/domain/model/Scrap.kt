package com.potatoes.book5.realestate.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** 한 번의 실거래. 가격은 만원 단위. */
data class Deal(
    val date: LocalDate,
    /** 전용면적(㎡)을 평으로 환산한 값 */
    val areaPyeong: Int,
    val floor: Int,
    val priceManwon: Long,
)

/** 비교에 쓰는 가격 기록(F-008 저장 데이터 설계). */
data class PriceRecord(val priceManwon: Long, val dealDate: LocalDate)

/** 스크랩 단지의 변동(F-008 규칙 3). */
data class PriceChange(
    val kind: ScrapChangeKind,
    /** 직전 확인 값 대비 차이(만원). 양수면 상승 */
    val deltaManwon: Long,
    val detectedAt: LocalDateTime,
    /** 사용자가 상세 화면에서 확인했는지(규칙 5) */
    val seen: Boolean,
)

/** 기기에 저장하는 스크랩(F-008, ADR-005). */
data class Scrap(
    val apartmentId: String,
    val name: String,
    val neighborhood: String,
    val areaPyeong: Int,
    val scrappedAt: LocalDateTime,
    /** 스크랩 당시 값 */
    val baseline: PriceRecord,
    /** 마지막으로 확인한 값 */
    val last: PriceRecord,
    val lastCheckedAt: LocalDateTime,
    val change: PriceChange?,
    /** 단지 세대수. 모르면 null(마이 페이지 오각형, T-017) */
    val households: Int? = null,
    /** 준공연도. 모르면 null */
    val builtYear: Int? = null,
) {
    val hasUnseenChange: Boolean get() = change?.seen == false
}

/** Local aggregate for a recommendation card that the user opened. No trip or raw event timestamp is kept. */
data class RecommendationFeedback(
    val apartmentId: String,
    val priceManwon: Long,
    val areaPyeong: Int,
    val neighborhoodName: String?,
    val households: Int?,
    val builtYear: Int?,
    val detailOpenCount: Int,
)

data class PricePoint(val month: YearMonth, val priceManwon: Long)

/** 아파트 상세(SCR-002). 제공되지 않는 단지 메타데이터는 null로 둔다. */
data class ApartmentDetail(
    val id: String,
    val name: String,
    val district: String,
    val neighborhood: String,
    val households: Int?,
    val builtYear: Int?,
    /** 전용면적을 환산한 평형 범위. 면적 정보가 없으면 null */
    val minPyeong: Int?,
    val maxPyeong: Int?,
    /** 화면에 명시한 기준점과의 직선거리. 좌표를 모르면 null */
    val distanceKm: Double?,
    val latestDeal: Deal,
    /** 최근 거래 직전의 거래. 첫 거래면 null */
    val previousDeal: Deal?,
    /** 조회기간 내 실제 월별 거래값. 제공된 월만 포함 */
    val trend: List<PricePoint>,
    /** 최근 거래 3건 */
    val recentDeals: List<Deal>,
)
