package com.potatoes.book5.realestate.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** 한 번의 실거래. 가격은 만원 단위. */
data class Deal(
    val date: LocalDate,
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

data class PricePoint(val month: YearMonth, val priceManwon: Long)

/** 아파트 상세(SCR-002). 실거래가 API(T-005) 연결 전에는 샘플 데이터다. */
data class ApartmentDetail(
    val id: String,
    val name: String,
    val district: String,
    val neighborhood: String,
    val households: Int,
    val builtYear: Int,
    val minPyeong: Int,
    val maxPyeong: Int,
    /** 현재 차량과의 거리. 위치를 모르면 null */
    val distanceKm: Double?,
    val latestDeal: Deal,
    /** 최근 거래 직전의 거래. 첫 거래면 null */
    val previousDeal: Deal?,
    /** 최근 1년 월별 실거래가 */
    val trend: List<PricePoint>,
    /** 최근 거래 3건 */
    val recentDeals: List<Deal>,
)
