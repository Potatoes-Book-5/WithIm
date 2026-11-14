package com.potatoes.book5.realestate.ui.summary

internal fun reduceDriveSummary(state: DriveSummaryState, result: DriveSummaryResult): DriveSummaryState = when (result) {
    is DriveSummaryResult.SummaryLoaded -> state.copy(isLoading = false, summary = result.summary)
    is DriveSummaryResult.ScrappedLoaded -> state.copy(scrappedIds = result.ids)
    is DriveSummaryResult.Toggling -> state.copy(
        togglingIds = if (result.inProgress) state.togglingIds + result.apartmentId else state.togglingIds - result.apartmentId,
    )
}
