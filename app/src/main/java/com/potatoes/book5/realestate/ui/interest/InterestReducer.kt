package com.potatoes.book5.realestate.ui.interest

internal fun reduceInterest(state: InterestState, result: InterestResult): InterestState = when (result) {
    is InterestResult.Loaded -> state.copy(
        isLoading = false,
        suggestions = result.suggestions,
        suggestionsFailed = false,
        selectedNeighborhoods = result.selection.neighborhoods,
        selectedPrices = result.selection.prices,
        selectedAreas = result.selection.areas,
        selectedHouseholds = result.selection.households,
        selectedBuildingAge = result.selection.buildingAge,
    )
    is InterestResult.LoadFailed -> state.copy(
        isLoading = false,
        suggestionsFailed = true,
        selectedNeighborhoods = result.selection.neighborhoods,
        selectedPrices = result.selection.prices,
        selectedAreas = result.selection.areas,
        selectedHouseholds = result.selection.households,
        selectedBuildingAge = result.selection.buildingAge,
    )
    is InterestResult.NeighborhoodToggled -> state.copy(selectedNeighborhoods = state.selectedNeighborhoods.toggle(result.neighborhood))
    is InterestResult.PriceToggled -> state.copy(selectedPrices = state.selectedPrices.toggle(result.range))
    is InterestResult.AreaToggled -> state.copy(selectedAreas = state.selectedAreas.toggle(result.range))
    is InterestResult.HouseholdsSelected -> state.copy(selectedHouseholds = result.min.takeUnless { it == state.selectedHouseholds })
    is InterestResult.BuildingAgeSelected -> state.copy(selectedBuildingAge = result.age.takeUnless { it == state.selectedBuildingAge })
    InterestResult.Saving -> state.copy(isSaving = true)
    InterestResult.SaveFailed -> state.copy(isSaving = false)
    InterestResult.Saved -> state.copy(isSaving = false)
}

private fun <T> Set<T>.toggle(item: T): Set<T> = if (item in this) this - item else this + item
