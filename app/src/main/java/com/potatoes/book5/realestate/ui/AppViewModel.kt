package com.potatoes.book5.realestate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.model.DrivingState
import com.potatoes.book5.realestate.domain.model.OnboardingStep
import com.potatoes.book5.realestate.domain.usecase.GetOnboardingStepUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveDrivingStateUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveUnseenDriveSummaryUseCase
import com.potatoes.book5.realestate.domain.usecase.RefreshScrapChangesUseCase
import com.potatoes.book5.realestate.domain.usecase.SimulateAreaChangeUseCase
import com.potatoes.book5.realestate.domain.usecase.SimulateDriveEndUseCase
import com.potatoes.book5.realestate.domain.usecase.SimulateDrivingUseCase
import com.potatoes.book5.realestate.domain.usecase.SimulateNewDealsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 앱 화면. 내비게이션 라이브러리 도입 전까지 화면 스택으로 전환한다. */
sealed interface Screen {
    data object Loading : Screen
    data object Consent : Screen
    data object Interest : Screen
    data object Home : Screen
    /** SCR-007. changedOnly면 '변동 있음' 필터로 연다. */
    data class Scraps(val changedOnly: Boolean = false) : Screen
    /** SCR-002 */
    data class Detail(val apartmentId: String) : Screen
    /** SCR-010 */
    data object MyPage : Screen
    /** SCR-006 설정(수정 모드) */
    data object Settings : Screen
    /** SCR-008. sessionId가 null이면 가장 최근 요약 */
    data class DriveSummary(val sessionId: Long? = null) : Screen
}

data class AppState(
    val stack: List<Screen> = listOf(Screen.Loading),
    val drivingState: DrivingState = DrivingState.PARKED,
) {
    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    /** 온보딩을 마쳤는지. 홈이 스택 맨 아래에 있으면 마친 것이다. */
    val onboardingDone: Boolean get() = stack.first() == Screen.Home

    /** 온보딩 이후 주행 중이면 어느 화면에 있었든 주행 화면을 보여 준다(S-005). 주차하면 원래 화면으로 돌아간다. */
    val showDriving: Boolean get() = onboardingDone && drivingState == DrivingState.DRIVING
}

sealed interface AppIntent {
    data object ConsentFinishedWithLocation : AppIntent
    data object OnboardingFinished : AppIntent
    data class Open(val screen: Screen) : AppIntent
    data object Back : AppIntent
    /** 개발 빌드 전용 시뮬레이션 */
    data class DevSetDriving(val driving: Boolean) : AppIntent
    data object DevMoveToNextArea : AppIntent
    data object DevPublishNewDeals : AppIntent
    data object DevEndDrive : AppIntent
}

sealed interface AppResult {
    data class StartAt(val step: OnboardingStep) : AppResult
    data class Reset(val stack: List<Screen>) : AppResult
    data class Push(val screen: Screen) : AppResult
    data object Pop : AppResult
    data class DrivingChanged(val state: DrivingState) : AppResult
    /** 확인하지 않은 주행 요약이 생겼다(F-009 규칙 3). */
    data class SummaryReady(val sessionId: Long) : AppResult
}

internal fun reduceApp(state: AppState, result: AppResult): AppState = when (result) {
    is AppResult.StartAt -> state.copy(
        stack = listOf(
            when (result.step) {
                OnboardingStep.CONSENT -> Screen.Consent
                OnboardingStep.INTEREST -> Screen.Interest
                OnboardingStep.DONE -> Screen.Home
            }
        ),
    )
    is AppResult.Reset -> state.copy(stack = result.stack)
    // 같은 화면을 연달아 쌓지 않는다.
    is AppResult.Push -> if (state.current == result.screen) state else state.copy(stack = state.stack + result.screen)
    AppResult.Pop -> if (state.canGoBack) state.copy(stack = state.stack.dropLast(1)) else state
    is AppResult.DrivingChanged -> state.copy(drivingState = result.state)
    // 온보딩 중에는 띄우지 않는다. 이미 요약 화면이면 다시 쌓지 않는다.
    is AppResult.SummaryReady -> if (!state.onboardingDone || state.current is Screen.DriveSummary) state
        else state.copy(stack = state.stack + Screen.DriveSummary(result.sessionId))
}

class AppViewModel(
    private val getOnboardingStep: GetOnboardingStepUseCase,
    private val observeDrivingState: ObserveDrivingStateUseCase,
    private val refreshScrapChanges: RefreshScrapChangesUseCase,
    private val simulateDriving: SimulateDrivingUseCase,
    private val simulateAreaChange: SimulateAreaChangeUseCase,
    private val simulateNewDeals: SimulateNewDealsUseCase,
    private val observeUnseenDriveSummary: ObserveUnseenDriveSummaryUseCase,
    private val simulateDriveEnd: SimulateDriveEndUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch { apply(AppResult.StartAt(getOnboardingStep())) }
        viewModelScope.launch { observeDrivingState().collect { apply(AppResult.DrivingChanged(it)) } }
        // 앱을 실행할 때 스크랩 단지의 변동을 확인한다(F-008 규칙 4).
        viewModelScope.launch { refreshScrapChanges() }
        // 주행 요약이 생기면 주차 중 화면 위에 띄운다. 앱이 꺼져 있었다면 다음 실행 때 띄운다(S-009).
        viewModelScope.launch {
            observeUnseenDriveSummary().collect { id -> if (id != null) apply(AppResult.SummaryReady(id)) }
        }
    }

    fun onIntent(intent: AppIntent) {
        when (intent) {
            AppIntent.ConsentFinishedWithLocation -> apply(AppResult.Reset(listOf(Screen.Interest)))
            AppIntent.OnboardingFinished -> apply(AppResult.Reset(listOf(Screen.Home)))
            is AppIntent.Open -> apply(AppResult.Push(intent.screen))
            AppIntent.Back -> apply(AppResult.Pop)
            is AppIntent.DevSetDriving -> simulateDriving(intent.driving)
            AppIntent.DevMoveToNextArea -> simulateAreaChange()
            AppIntent.DevPublishNewDeals -> viewModelScope.launch { simulateNewDeals() }
            AppIntent.DevEndDrive -> simulateDriveEnd()
        }
    }

    private fun apply(result: AppResult) {
        _state.update { reduceApp(it, result) }
    }
}
