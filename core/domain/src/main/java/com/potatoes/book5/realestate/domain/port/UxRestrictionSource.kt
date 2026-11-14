package com.potatoes.book5.realestate.domain.port

import kotlinx.coroutines.flow.Flow

/**
 * 플랫폼 주행 규제 상태(T-035, DI-001, DI-002). 구현은 vehicle/ 패키지의 `CarUxRestrictionsManager` 어댑터다.
 * 수집하는 동안 차량 서비스에 연결하고, 수집을 멈추면 해제한다.
 */
interface UxRestrictionSource {
    val state: Flow<UxRestrictionState>
}

enum class UxRestrictionState {
    /** `isRequiresDistractionOptimization()`이 false */
    UNRESTRICTED,

    /** `isRequiresDistractionOptimization()`이 true. DO 화면만 보여야 한다. */
    RESTRICTED,

    /** 차량 서비스가 없거나 연결하지 못했다. 기어 기준 주행 상태만 쓴다. */
    UNAVAILABLE,
}
