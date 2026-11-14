package com.potatoes.book5.realestate.ui.consent

data class ConsentState(
    val interestAlertEnabled: Boolean = true,
    val leaderAlertEnabled: Boolean = true,
    val isSaving: Boolean = false,
    /** 홈의 설정에서 연 수정 모드(SCR-006/settings). 첫 실행 동의와 버튼, 항목이 다르다. */
    val editing: Boolean = false,
    /** 수정 모드에서 저장된 값을 불러오는 중 */
    val isLoading: Boolean = false,
    /** 수정 모드의 차량 위치 사용 토글 */
    val locationConsented: Boolean = true,
    val confirmingDelete: Boolean = false,
    val isDeleting: Boolean = false,
)

sealed interface ConsentIntent {
    data class SetInterestAlert(val enabled: Boolean) : ConsentIntent
    data class SetLeaderAlert(val enabled: Boolean) : ConsentIntent
    /** 동의하고 시작하기 */
    data object Agree : ConsentIntent
    /** 나중에: 위치 사용에 동의하지 않고 시작(S-002 예외 흐름) */
    data object Decline : ConsentIntent

    // 수정 모드
    data class SetLocation(val consented: Boolean) : ConsentIntent
    data object Save : ConsentIntent
    data object Cancel : ConsentIntent
    data object RequestDeleteData : ConsentIntent
    data object DismissDeleteData : ConsentIntent
    data object ConfirmDeleteData : ConsentIntent
}

sealed interface ConsentResult {
    data class InterestAlertChanged(val enabled: Boolean) : ConsentResult
    data class LeaderAlertChanged(val enabled: Boolean) : ConsentResult
    data class LocationChanged(val consented: Boolean) : ConsentResult
    data class Loaded(val locationConsented: Boolean, val interestAlertEnabled: Boolean, val leaderAlertEnabled: Boolean) : ConsentResult
    data object Saving : ConsentResult
    data object Saved : ConsentResult
    data class DeleteConfirming(val showing: Boolean) : ConsentResult
    data object Deleting : ConsentResult
    data object Deleted : ConsentResult
}

sealed interface ConsentEffect {
    data object OpenInterestConditions : ConsentEffect
    data object OpenHome : ConsentEffect
    /** 수정 모드를 닫고 이전 화면으로 */
    data object Close : ConsentEffect
    data object ShowDataDeleted : ConsentEffect
    /** 위치 사용에 동의하고 저장했다. FusedLocation 런타임 권한을 요청한다(F-001, T-029). 화면 이동 Effect보다 먼저 보낸다. */
    data object RequestLocationPermission : ConsentEffect
}
