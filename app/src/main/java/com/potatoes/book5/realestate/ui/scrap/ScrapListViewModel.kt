package com.potatoes.book5.realestate.ui.scrap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.usecase.ObserveScrapsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScrapListViewModel(
    initialFilter: ScrapFilter,
    private val observeScraps: ObserveScrapsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ScrapListState(filter = initialFilter))
    val state: StateFlow<ScrapListState> = _state.asStateFlow()

    private val _effect = Channel<ScrapListEffect>(Channel.BUFFERED)
    val effect: Flow<ScrapListEffect> = _effect.receiveAsFlow()

    init {
        // DB를 관찰하므로 상세에서 확인하거나 해제한 결과가 돌아왔을 때 바로 반영된다.
        viewModelScope.launch {
            observeScraps().collect { scraps -> apply(ScrapListResult.Loaded(scraps.map { it.toItemUi() })) }
        }
    }

    fun onIntent(intent: ScrapListIntent) {
        when (intent) {
            is ScrapListIntent.SelectFilter -> apply(ScrapListResult.FilterSelected(intent.filter))
            is ScrapListIntent.OpenItem -> send(ScrapListEffect.OpenDetail(intent.id))
            ScrapListIntent.Back -> send(ScrapListEffect.Close)
        }
    }

    private fun send(effect: ScrapListEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun apply(result: ScrapListResult) {
        _state.update { reduceScrapList(it, result) }
    }
}
