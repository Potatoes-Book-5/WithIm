package com.potatoes.book5.realestate.domain.model

/** 첫 실행 동의 결과와 알림 설정(F-007). */
data class ConsentSettings(
    val locationConsented: Boolean,
    val interestAlertEnabled: Boolean,
    val leaderAlertEnabled: Boolean,
)

/** 첫 실행 흐름에서 다음에 보여 줄 단계. */
enum class OnboardingStep { CONSENT, INTEREST, DONE }

/** 동네. 코드는 데이터 출처가 정해지면(T-005) 행정구역 코드로 바뀝니다. */
data class Neighborhood(val code: String, val name: String)

/** 가격대 구간. 구간 값은 확정 전 제안값입니다(F-004). */
enum class PriceRange { UNDER_5, FROM_5_TO_10, FROM_10_TO_15, FROM_15_TO_20, OVER_20 }

/** 평수 구간. 전용·공급 기준은 미정입니다(F-004). */
enum class AreaRange { UNDER_20, PY_20S, PY_30S, OVER_40 }

/** 관심 조건(F-004). 하나 이상 골라야 저장할 수 있습니다. */
data class InterestCondition(
    val neighborhoods: Set<Neighborhood> = emptySet(),
    val priceRanges: Set<PriceRange> = emptySet(),
    val areaRanges: Set<AreaRange> = emptySet(),
) {
    val isEmpty: Boolean
        get() = neighborhoods.isEmpty() && priceRanges.isEmpty() && areaRanges.isEmpty()
}
