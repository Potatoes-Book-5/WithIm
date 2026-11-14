package com.potatoes.book5.realestate.ui.consent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.potatoes.book5.realestate.ui.AppViewModels
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle

/** SCR-006 동의와 설정. ViewModel과 Effect를 연결한다. */
@Composable
fun ConsentRoute(
    onOpenInterestConditions: () -> Unit,
    onOpenHome: () -> Unit,
    viewModel: ConsentViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ConsentEffect.OpenInterestConditions -> onOpenInterestConditions()
                ConsentEffect.OpenHome -> onOpenHome()
            }
        }
    }
    ConsentScreen(state = state, onIntent = viewModel::onIntent)
}
