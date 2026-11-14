package com.potatoes.book5.realestate.ui.detail

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

/** SCR-002 아파트 상세. ViewModel과 Effect를 연결한다. */
@Composable
fun DetailRoute(
    apartmentId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = viewModel(
        factory = AppViewModels.Factory,
        extras = AppViewModels.extrasWith(AppViewModels.APARTMENT_ID_KEY, apartmentId),
    ),
) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                DetailEffect.Close -> onBack()
                DetailEffect.ShowScrapFailed -> {
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(context.getString(R.string.detail_scrap_failed))
                }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        DetailScreen(state = state, onIntent = viewModel::onIntent)
        LargeSnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.wpx),
        )
    }
}
