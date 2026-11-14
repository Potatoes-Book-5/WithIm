package com.potatoes.book5.realestate.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.usecase.GetApartmentDetailUseCase
import com.potatoes.book5.realestate.domain.usecase.GetScrapUseCase
import com.potatoes.book5.realestate.domain.usecase.MarkScrapChangeSeenUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveIsScrappedUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DetailViewModel(
    private val apartmentId: String,
    private val getApartmentDetail: GetApartmentDetailUseCase,
    private val getScrap: GetScrapUseCase,
    private val observeIsScrapped: ObserveIsScrappedUseCase,
    private val toggleScrap: ToggleScrapUseCase,
    private val markScrapChangeSeen: MarkScrapChangeSeenUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    private val _effect = Channel<DetailEffect>(Channel.BUFFERED)
    val effect: Flow<DetailEffect> = _effect.receiveAsFlow()

    init {
        load()
        viewModelScope.launch {
            observeIsScrapped(apartmentId).collect { apply(DetailResult.ScrapStateChanged(it)) }
        }
    }

    fun onIntent(intent: DetailIntent) {
        when (intent) {
            DetailIntent.ToggleScrap -> toggle()
            DetailIntent.Retry -> load()
            DetailIntent.Back -> viewModelScope.launch { _effect.send(DetailEffect.Close) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            apply(DetailResult.Loading)
            when (val outcome = getApartmentDetail(apartmentId)) {
                is Outcome.Success -> {
                    val unseen = getScrap(apartmentId)?.change?.takeIf { !it.seen }
                    apply(DetailResult.Loaded(outcome.value, unseen?.kind, deltaFor(outcome.value, unseen)))
                    // 상세를 열었으므로 변동을 확인한 것으로 기록한다(F-008 규칙 5).
                    if (unseen != null) markScrapChangeSeen(apartmentId)
                }
                is Outcome.Failure -> apply(DetailResult.Failed)
            }
        }
    }

    private fun toggle() {
        val current = _state.value
        val detail = current.detail ?: return
        if (current.isToggling) return
        viewModelScope.launch {
            apply(DetailResult.Toggling(true))
            try {
                toggleScrap(detail)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effect.send(DetailEffect.ShowScrapFailed)
            } finally {
                apply(DetailResult.Toggling(false))
            }
        }
    }

    private fun apply(result: DetailResult) {
        _state.update { reduceDetail(it, result) }
    }
}
