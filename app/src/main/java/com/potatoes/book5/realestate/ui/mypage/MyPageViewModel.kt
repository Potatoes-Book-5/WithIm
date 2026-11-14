package com.potatoes.book5.realestate.ui.mypage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.potatoes.book5.realestate.domain.usecase.ObserveMyPageUseCase
import com.potatoes.book5.realestate.ui.Screen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MyPageViewModel(private val observeMyPage: ObserveMyPageUseCase) : ViewModel() {

    private val _state = MutableStateFlow(MyPageState())
    val state: StateFlow<MyPageState> = _state.asStateFlow()

    private val _effect = Channel<MyPageEffect>(Channel.BUFFERED)
    val effect: Flow<MyPageEffect> = _effect.receiveAsFlow()

    init {
        // 조건과 스크랩을 관찰하므로 조건 수정이나 상세에서 돌아오면 바로 다시 계산된다.
        viewModelScope.launch {
            observeMyPage().collect { data -> _state.update { reduceMyPage(it, data.toResult()) } }
        }
    }

    fun onIntent(intent: MyPageIntent) {
        val effect = when (intent) {
            MyPageIntent.Back -> MyPageEffect.Close
            MyPageIntent.EditConditions -> MyPageEffect.Navigate(Screen.Interest)
            is MyPageIntent.OpenApartment -> MyPageEffect.Navigate(Screen.Detail(intent.id))
            MyPageIntent.OpenScraps -> MyPageEffect.Navigate(Screen.Scraps())
        }
        viewModelScope.launch { _effect.send(effect) }
    }
}
