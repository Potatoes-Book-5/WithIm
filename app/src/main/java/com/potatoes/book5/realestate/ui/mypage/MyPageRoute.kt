package com.potatoes.book5.realestate.ui.mypage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.potatoes.book5.realestate.ui.AppViewModels
import com.potatoes.book5.realestate.ui.Screen
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle

/** SCR-010 마이 페이지. ViewModel과 Effect를 연결한다. */
@Composable
fun MyPageRoute(
    onOpen: (Screen) -> Unit,
    onBack: () -> Unit,
    viewModel: MyPageViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is MyPageEffect.Navigate -> onOpen(effect.screen)
                MyPageEffect.Close -> onBack()
            }
        }
    }
    MyPageScreen(state = state, onIntent = viewModel::onIntent)
}
