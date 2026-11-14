package com.potatoes.book5.realestate.ui.interest

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.usecase.GetInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.GetNeighborhoodSuggestionsUseCase
import com.potatoes.book5.realestate.domain.usecase.SaveInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.SkipInterestConditionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InterestViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val getSuggestions: GetNeighborhoodSuggestionsUseCase,
    private val getInterestCondition: GetInterestConditionUseCase,
    private val saveInterestCondition: SaveInterestConditionUseCase,
    private val skipInterestCondition: SkipInterestConditionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(InterestState())
    val state: StateFlow<InterestState> = _state.asStateFlow()

    private val _effect = Channel<InterestEffect>(Channel.BUFFERED)
    val effect: Flow<InterestEffect> = _effect.receiveAsFlow()

    init {
        load()
    }

    fun onIntent(intent: InterestIntent) {
        when (intent) {
            is InterestIntent.ToggleNeighborhood -> apply(InterestResult.NeighborhoodToggled(intent.neighborhood))
            is InterestIntent.TogglePrice -> apply(InterestResult.PriceToggled(intent.range))
            is InterestIntent.ToggleArea -> apply(InterestResult.AreaToggled(intent.range))
            InterestIntent.VoiceSearch -> send(InterestEffect.ShowMessage(InterestMessage.VOICE_UNAVAILABLE))
            InterestIntent.Save -> save()
            InterestIntent.Skip -> skip()
        }
    }

    private fun load() {
        viewModelScope.launch {
            // 프로세스 재생성 후에는 화면에서 고르던 값을, 아니면 저장된 조건을 쓴다(SM-009).
            val selection = restoreSelection() ?: getInterestCondition()?.toSelection() ?: InterestSelection()
            when (val outcome = getSuggestions()) {
                is Outcome.Success -> apply(InterestResult.Loaded(outcome.value.map { it.toUi() }, selection))
                is Outcome.Failure -> apply(InterestResult.LoadFailed(selection))
            }
        }
    }

    private fun save() {
        val current = _state.value
        if (!current.canSave) return
        viewModelScope.launch {
            apply(InterestResult.Saving)
            val condition = InterestCondition(
                neighborhoods = current.selectedNeighborhoods.map { Neighborhood(it.code, it.name) }.toSet(),
                priceRanges = current.selectedPrices,
                areaRanges = current.selectedAreas,
            )
            when (saveInterestCondition(condition)) {
                is Outcome.Success -> {
                    apply(InterestResult.Saved)
                    send(InterestEffect.OpenHome)
                }
                is Outcome.Failure -> {
                    apply(InterestResult.SaveFailed)
                    send(InterestEffect.ShowMessage(InterestMessage.SAVE_FAILED))
                }
            }
        }
    }

    private fun skip() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            skipInterestCondition()
            send(InterestEffect.OpenHome)
        }
    }

    private fun apply(result: InterestResult) {
        _state.update { reduceInterest(it, result) }
        persistSelection(_state.value)
    }

    private fun send(effect: InterestEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun persistSelection(state: InterestState) {
        if (state.isLoading) return
        savedStateHandle[KEY_NEIGHBORHOODS] = ArrayList(state.selectedNeighborhoods.map { "${it.code}$SEP${it.name}" })
        savedStateHandle[KEY_PRICES] = ArrayList(state.selectedPrices.map { it.name })
        savedStateHandle[KEY_AREAS] = ArrayList(state.selectedAreas.map { it.name })
    }

    private fun restoreSelection(): InterestSelection? {
        val neighborhoods = savedStateHandle.get<ArrayList<String>>(KEY_NEIGHBORHOODS) ?: return null
        return InterestSelection(
            neighborhoods = neighborhoods.mapNotNull { raw ->
                raw.split(SEP, limit = 2).takeIf { it.size == 2 }?.let { NeighborhoodUi(it[0], it[1]) }
            }.toSet(),
            prices = savedStateHandle.get<ArrayList<String>>(KEY_PRICES).orEmpty()
                .mapNotNull { name -> PriceRange.entries.firstOrNull { it.name == name } }.toSet(),
            areas = savedStateHandle.get<ArrayList<String>>(KEY_AREAS).orEmpty()
                .mapNotNull { name -> AreaRange.entries.firstOrNull { it.name == name } }.toSet(),
        )
    }

    private fun Neighborhood.toUi() = NeighborhoodUi(code, name)

    private fun InterestCondition.toSelection() = InterestSelection(
        neighborhoods = neighborhoods.map { it.toUi() }.toSet(),
        prices = priceRanges,
        areas = areaRanges,
    )

    private companion object {
        const val SEP = "|"
        const val KEY_NEIGHBORHOODS = "interest_neighborhoods"
        const val KEY_PRICES = "interest_prices"
        const val KEY_AREAS = "interest_areas"
    }
}
