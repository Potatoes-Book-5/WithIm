package com.potatoes.book5.realestate.ui.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.usecase.DeleteUserDataUseCase
import com.potatoes.book5.realestate.domain.usecase.GetConsentUseCase
import com.potatoes.book5.realestate.domain.usecase.SaveConsentUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * SCR-006. 첫 실행에는 동의를 받고, [editing]이면 홈의 설정에서 연 수정 모드로 동작한다.
 * 수정 모드는 저장된 값을 불러와 저장 또는 취소하고, 저장된 데이터를 지울 수 있다(F-007 규칙 3).
 */
class ConsentViewModel(
    private val saveConsent: SaveConsentUseCase,
    private val getConsent: GetConsentUseCase,
    private val deleteUserData: DeleteUserDataUseCase,
    private val editing: Boolean = false,
) : ViewModel() {

    private val _state = MutableStateFlow(ConsentState(editing = editing, isLoading = editing))
    val state: StateFlow<ConsentState> = _state.asStateFlow()

    private val _effect = Channel<ConsentEffect>(Channel.BUFFERED)
    val effect: Flow<ConsentEffect> = _effect.receiveAsFlow()

    init {
        if (editing) {
            viewModelScope.launch {
                // 동의 화면에서 결정하지 않은 채 들어오는 경우는 없지만, 없으면 기본값으로 시작한다.
                val stored = getConsent() ?: ConsentSettings(locationConsented = false, interestAlertEnabled = true, leaderAlertEnabled = true)
                apply(ConsentResult.Loaded(stored.locationConsented, stored.interestAlertEnabled, stored.leaderAlertEnabled))
            }
        }
    }

    fun onIntent(intent: ConsentIntent) {
        when (intent) {
            is ConsentIntent.SetInterestAlert -> apply(ConsentResult.InterestAlertChanged(intent.enabled))
            is ConsentIntent.SetLeaderAlert -> apply(ConsentResult.LeaderAlertChanged(intent.enabled))
            is ConsentIntent.SetLocation -> apply(ConsentResult.LocationChanged(intent.consented))
            ConsentIntent.Agree -> save(locationConsented = true, next = ConsentEffect.OpenInterestConditions)
            ConsentIntent.Decline -> save(locationConsented = false, next = ConsentEffect.OpenHome)
            ConsentIntent.Save -> if (!_state.value.isLoading) save(locationConsented = _state.value.locationConsented, next = ConsentEffect.Close)
            ConsentIntent.Cancel -> send(ConsentEffect.Close)
            ConsentIntent.RequestDeleteData -> apply(ConsentResult.DeleteConfirming(true))
            ConsentIntent.DismissDeleteData -> apply(ConsentResult.DeleteConfirming(false))
            ConsentIntent.ConfirmDeleteData -> deleteData()
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
            if (locationConsented) _effect.send(ConsentEffect.RequestLocationPermission)
            _effect.send(next)
        }
    }

    private fun deleteData() {
        if (_state.value.isDeleting) return
        viewModelScope.launch {
            apply(ConsentResult.Deleting)
            deleteUserData()
            apply(ConsentResult.Deleted)
            _effect.send(ConsentEffect.ShowDataDeleted)
        }
    }

    private fun send(effect: ConsentEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun apply(result: ConsentResult) {
        _state.update { reduceConsent(it, result) }
    }
}
