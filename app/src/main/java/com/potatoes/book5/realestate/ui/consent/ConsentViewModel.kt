package com.potatoes.book5.realestate.ui.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.usecase.SaveConsentUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConsentViewModel(
    private val saveConsent: SaveConsentUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ConsentState())
    val state: StateFlow<ConsentState> = _state.asStateFlow()

    private val _effect = Channel<ConsentEffect>(Channel.BUFFERED)
    val effect: Flow<ConsentEffect> = _effect.receiveAsFlow()

    fun onIntent(intent: ConsentIntent) {
        when (intent) {
            is ConsentIntent.SetInterestAlert -> apply(ConsentResult.InterestAlertChanged(intent.enabled))
            is ConsentIntent.SetLeaderAlert -> apply(ConsentResult.LeaderAlertChanged(intent.enabled))
            ConsentIntent.Agree -> save(locationConsented = true, next = ConsentEffect.OpenInterestConditions)
            ConsentIntent.Decline -> save(locationConsented = false, next = ConsentEffect.OpenHome)
        }
    }

    private fun save(locationConsented: Boolean, next: ConsentEffect) {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            apply(ConsentResult.Saving)
            val current = _state.value
            saveConsent(
                ConsentSettings(
                    locationConsented = locationConsented,
                    interestAlertEnabled = current.interestAlertEnabled,
                    leaderAlertEnabled = current.leaderAlertEnabled,
                )
            )
            apply(ConsentResult.Saved)
            _effect.send(next)
        }
    }

    private fun apply(result: ConsentResult) {
        _state.update { reduceConsent(it, result) }
    }
}
