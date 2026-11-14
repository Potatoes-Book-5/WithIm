package com.potatoes.book5.realestate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.model.OnboardingStep
import com.potatoes.book5.realestate.domain.usecase.GetOnboardingStepUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 앱 전체에서 보여 줄 화면. 내비게이션 라이브러리 도입 전까지 상태로 전환한다. */
enum class Destination { LOADING, CONSENT, INTEREST, HOME }

data class AppState(
    val destination: Destination = Destination.LOADING,
    /** 홈에서 관심 조건 화면을 열었으면 뒤로 가기로 홈에 돌아간다. */
    val interestOpenedFromHome: Boolean = false,
)

sealed interface AppIntent {
    data object ConsentFinishedWithLocation : AppIntent
    data object OnboardingFinished : AppIntent
    data object OpenInterestFromHome : AppIntent
    data object Back : AppIntent
}

sealed interface AppResult {
    data class StartAt(val step: OnboardingStep) : AppResult
    data class Show(val destination: Destination, val fromHome: Boolean = false) : AppResult
}

internal fun reduceApp(state: AppState, result: AppResult): AppState = when (result) {
    is AppResult.StartAt -> state.copy(
        destination = when (result.step) {
            OnboardingStep.CONSENT -> Destination.CONSENT
            OnboardingStep.INTEREST -> Destination.INTEREST
            OnboardingStep.DONE -> Destination.HOME
        },
        interestOpenedFromHome = false,
    )
    is AppResult.Show -> state.copy(destination = result.destination, interestOpenedFromHome = result.fromHome)
}

class AppViewModel(
    private val getOnboardingStep: GetOnboardingStepUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch { apply(AppResult.StartAt(getOnboardingStep())) }
    }

    fun onIntent(intent: AppIntent) {
        when (intent) {
            AppIntent.ConsentFinishedWithLocation -> apply(AppResult.Show(Destination.INTEREST))
            AppIntent.OnboardingFinished -> apply(AppResult.Show(Destination.HOME))
            AppIntent.OpenInterestFromHome -> apply(AppResult.Show(Destination.INTEREST, fromHome = true))
            AppIntent.Back -> if (_state.value.interestOpenedFromHome) apply(AppResult.Show(Destination.HOME))
        }
    }

    private fun apply(result: AppResult) {
        _state.update { reduceApp(it, result) }
    }
}
