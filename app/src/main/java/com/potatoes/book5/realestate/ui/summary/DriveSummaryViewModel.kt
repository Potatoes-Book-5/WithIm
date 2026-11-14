package com.potatoes.book5.realestate.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.usecase.MarkDriveSummarySeenUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveDriveSummaryUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrappedIdsUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapByIdUseCase
import com.potatoes.book5.realestate.ui.Screen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** @param sessionId 보여 줄 요약. null이면 가장 최근 요약 */
class DriveSummaryViewModel(
    sessionId: Long?,
    observeDriveSummary: ObserveDriveSummaryUseCase,
    observeScrappedIds: ObserveScrappedIdsUseCase,
    private val markSeen: MarkDriveSummarySeenUseCase,
    private val toggleScrapById: ToggleScrapByIdUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DriveSummaryState())
    val state: StateFlow<DriveSummaryState> = _state.asStateFlow()

    private val _effect = Channel<DriveSummaryEffect>(Channel.BUFFERED)
    val effect: Flow<DriveSummaryEffect> = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeDriveSummary(sessionId).collect { summary ->
                apply(DriveSummaryResult.SummaryLoaded(summary))
                // 요약을 열었으면 확인한 것으로 기록해 다시 자동으로 띄우지 않는다.
                if (summary != null && !summary.seen) markSeen(summary.id)
            }
        }
        viewModelScope.launch {
            observeScrappedIds().collect { apply(DriveSummaryResult.ScrappedLoaded(it)) }
        }
    }

    fun onIntent(intent: DriveSummaryIntent) {
        when (intent) {
            DriveSummaryIntent.Close -> send(DriveSummaryEffect.Close)
            is DriveSummaryIntent.OpenApartment -> send(DriveSummaryEffect.Navigate(Screen.Detail(intent.apartmentId)))
            DriveSummaryIntent.OpenScraps -> send(DriveSummaryEffect.Navigate(Screen.Scraps()))
            DriveSummaryIntent.OpenInterestCatchUp -> _state.value.summary?.let { send(DriveSummaryEffect.Navigate(Screen.InterestCatchUp(it.id))) }
            is DriveSummaryIntent.ToggleScrap -> toggle(intent.apartmentId)
        }
    }

    private fun toggle(apartmentId: String) {
        if (apartmentId in _state.value.togglingIds) return
        viewModelScope.launch {
            apply(DriveSummaryResult.Toggling(apartmentId, true))
            val ok = try {
                toggleScrapById(apartmentId) is Outcome.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            if (!ok) _effect.send(DriveSummaryEffect.ShowScrapFailed)
            apply(DriveSummaryResult.Toggling(apartmentId, false))
        }
    }

    private fun send(effect: DriveSummaryEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun apply(result: DriveSummaryResult) {
        _state.update { reduceDriveSummary(it, result) }
    }
}
