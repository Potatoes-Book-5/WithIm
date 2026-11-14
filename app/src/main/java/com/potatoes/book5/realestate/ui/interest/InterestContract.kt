package com.potatoes.book5.realestate.ui.interest

import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.PriceRange

data class NeighborhoodUi(val code: String, val name: String)

data class InterestState(
    val isLoading: Boolean = true,
    val suggestions: List<NeighborhoodUi> = emptyList(),
    val suggestionsFailed: Boolean = false,
    val selectedNeighborhoods: Set<NeighborhoodUi> = emptySet(),
    val selectedPrices: Set<PriceRange> = emptySet(),
    val selectedAreas: Set<AreaRange> = emptySet(),
    val isSaving: Boolean = false,
) {
    val hasSelection: Boolean
        get() = selectedNeighborhoods.isNotEmpty() || selectedPrices.isNotEmpty() || selectedAreas.isNotEmpty()

    /** 조건이 하나 이상이어야 저장할 수 있다(F-004 규칙 2). */
    val canSave: Boolean get() = hasSelection && !isSaving

    /** 추천 동네와, 추천에 없지만 이미 고른 동네(이전에 저장한 조건)를 함께 보여 준다. */
    val neighborhoodChips: List<NeighborhoodUi>
        get() = suggestions + selectedNeighborhoods.filterNot { it in suggestions }
}

sealed interface InterestIntent {
    data class ToggleNeighborhood(val neighborhood: NeighborhoodUi) : InterestIntent
    data class TogglePrice(val range: PriceRange) : InterestIntent
    data class ToggleArea(val range: AreaRange) : InterestIntent
    data object VoiceSearch : InterestIntent
    data object Save : InterestIntent
    data object Skip : InterestIntent
}

data class InterestSelection(
    val neighborhoods: Set<NeighborhoodUi> = emptySet(),
    val prices: Set<PriceRange> = emptySet(),
    val areas: Set<AreaRange> = emptySet(),
)

sealed interface InterestResult {
    data class Loaded(val suggestions: List<NeighborhoodUi>, val selection: InterestSelection) : InterestResult
    data class LoadFailed(val selection: InterestSelection) : InterestResult
    data class NeighborhoodToggled(val neighborhood: NeighborhoodUi) : InterestResult
    data class PriceToggled(val range: PriceRange) : InterestResult
    data class AreaToggled(val range: AreaRange) : InterestResult
    data object Saving : InterestResult
    data object SaveFailed : InterestResult
    data object Saved : InterestResult
}

enum class InterestMessage { VOICE_UNAVAILABLE, SAVE_FAILED }

sealed interface InterestEffect {
    data object OpenHome : InterestEffect
    data class ShowMessage(val message: InterestMessage) : InterestEffect
}
