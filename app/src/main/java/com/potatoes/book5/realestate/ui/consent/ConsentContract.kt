package com.potatoes.book5.realestate.ui.consent

data class ConsentState(
    val interestAlertEnabled: Boolean = true,
    val leaderAlertEnabled: Boolean = true,
    val isSaving: Boolean = false,
)

sealed interface ConsentIntent {
    data class SetInterestAlert(val enabled: Boolean) : ConsentIntent
    data class SetLeaderAlert(val enabled: Boolean) : ConsentIntent
    /** 동의하고 시작하기 */
    data object Agree : ConsentIntent
    /** 나중에: 위치 사용에 동의하지 않고 시작(S-002 예외 흐름) */
    data object Decline : ConsentIntent
}

sealed interface ConsentResult {
    data class InterestAlertChanged(val enabled: Boolean) : ConsentResult
    data class LeaderAlertChanged(val enabled: Boolean) : ConsentResult
    data object Saving : ConsentResult
    data object Saved : ConsentResult
}

sealed interface ConsentEffect {
    data object OpenInterestConditions : ConsentEffect
    data object OpenHome : ConsentEffect
}
