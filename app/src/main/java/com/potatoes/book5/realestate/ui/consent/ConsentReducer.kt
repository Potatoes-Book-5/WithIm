package com.potatoes.book5.realestate.ui.consent

internal fun reduceConsent(state: ConsentState, result: ConsentResult): ConsentState = when (result) {
    is ConsentResult.InterestAlertChanged -> state.copy(interestAlertEnabled = result.enabled)
    is ConsentResult.LeaderAlertChanged -> state.copy(leaderAlertEnabled = result.enabled)
    ConsentResult.Saving -> state.copy(isSaving = true)
    ConsentResult.Saved -> state.copy(isSaving = false)
}
