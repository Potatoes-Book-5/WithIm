package com.potatoes.book5.realestate.ui.home

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
import com.potatoes.book5.realestate.ui.Screen
import com.potatoes.book5.realestate.ui.common.LargeSnackbarHost
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle
import com.potatoes.book5.realestate.ui.theme.wpx

/** SCR-001 홈. ViewModel과 Effect를 연결한다. */
@Composable
fun HomeRoute(
    onOpen: (Screen) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.Navigate -> onOpen(effect.screen)
                is HomeEffect.ShowComingSoon -> {
                    val text = when (effect.destination) {
                        HomeDestination.MY_PAGE -> R.string.coming_soon_my_page
                        HomeDestination.SETTINGS -> R.string.coming_soon_settings
                        HomeDestination.DRIVE_SUMMARY -> R.string.coming_soon_drive_summary
                    }
                    snackbar.currentSnackbarData?.dismiss()
                    snackbar.showSnackbar(context.getString(text))
                }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        HomeScreen(state = state, onIntent = viewModel::onIntent)
        LargeSnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.wpx),
        )
    }
}
