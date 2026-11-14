package com.potatoes.book5.realestate.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.potatoes.book5.realestate.ui.AppViewModels
import com.potatoes.book5.realestate.ui.Screen
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle

/** SCR-001 홈. ViewModel과 Effect를 연결한다. */
@Composable
fun HomeRoute(
    onOpen: (Screen) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.Navigate -> onOpen(effect.screen)
            }
        }
    }
    HomeScreen(state = state, onIntent = viewModel::onIntent)
}
