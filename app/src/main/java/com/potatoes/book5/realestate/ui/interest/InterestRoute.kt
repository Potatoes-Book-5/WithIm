package com.potatoes.book5.realestate.ui.interest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle
import com.potatoes.book5.realestate.ui.theme.wpx

/** SCR-003 관심 조건. ViewModel과 Effect를 연결한다. */
@Composable
fun InterestRoute(
    onOpenHome: () -> Unit,
    viewModel: InterestViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                InterestEffect.OpenHome -> onOpenHome()
                is InterestEffect.ShowMessage -> {
                    val text = when (effect.message) {
                        InterestMessage.VOICE_UNAVAILABLE -> R.string.interest_voice_unavailable
                        InterestMessage.SAVE_FAILED -> R.string.interest_save_failed
                    }
                    snackbar.showSnackbar(context.getString(text))
                }
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        InterestScreen(state = state, onIntent = viewModel::onIntent)
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 200.wpx),
        ) { data ->
            // 기본 스낵바 글자는 이 화면 밀도에서 너무 작아 직접 그린다.
            Surface(
                shape = RoundedCornerShape(36.wpx),
                color = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 48.wpx, vertical = 28.wpx),
                )
            }
        }
    }
}
