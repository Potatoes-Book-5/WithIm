package com.potatoes.book5.realestate.ui.consent

internal fun reduceConsent(state: ConsentState, result: ConsentResult): ConsentState = when (result) {
    is ConsentResult.InterestAlertChanged -> state.copy(interestAlertEnabled = result.enabled)
    is ConsentResult.LeaderAlertChanged -> state.copy(leaderAlertEnabled = result.enabled)
    is ConsentResult.LocationChanged -> state.copy(locationConsented = result.consented)
    is ConsentResult.Loaded -> state.copy(
        isLoading = false,
        locationConsented = result.locationConsented,
        interestAlertEnabled = result.interestAlertEnabled,
        leaderAlertEnabled = result.leaderAlertEnabled,
    )
    ConsentResult.Saving -> state.copy(isSaving = true)
    ConsentResult.Saved -> state.copy(isSaving = false)
    is ConsentResult.DeleteConfirming -> state.copy(confirmingDelete = result.showing)
    ConsentResult.Deleting -> state.copy(confirmingDelete = false, isDeleting = true)
    ConsentResult.Deleted -> state.copy(isDeleting = false)
}
