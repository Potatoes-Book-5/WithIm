package com.potatoes.book5.realestate.ui.consent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.potatoes.book5.realestate.R
import com.potatoes.book5.realestate.ui.AppViewModels
import com.potatoes.book5.realestate.ui.common.LargeSnackbarHost
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle
import com.potatoes.book5.realestate.ui.theme.wpx

/** SCR-006 동의와 설정. ViewModel과 Effect를 연결한다. [editing]이면 홈의 설정에서 연 수정 모드다. */
@Composable
fun ConsentRoute(
    onOpenInterestConditions: () -> Unit,
    onOpenHome: () -> Unit,
    editing: Boolean = false,
    onClose: () -> Unit = {},
    viewModel: ConsentViewModel = viewModel(
        factory = AppViewModels.Factory,
        extras = AppViewModels.extrasWith(AppViewModels.SETTINGS_EDITING_KEY, editing),
    ),
) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ConsentEffect.OpenInterestConditions -> onOpenInterestConditions()
                ConsentEffect.OpenHome -> onOpenHome()
                ConsentEffect.Close -> onClose()
                ConsentEffect.ShowDataDeleted -> {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(context.getString(R.string.settings_data_deleted))
                }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        ConsentScreen(state = state, onIntent = viewModel::onIntent)
        LargeSnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 200.wpx))
    }
}
