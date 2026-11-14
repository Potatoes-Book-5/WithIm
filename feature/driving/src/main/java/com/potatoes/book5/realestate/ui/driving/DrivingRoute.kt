package com.potatoes.book5.realestate.ui.driving

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle

/**
 * SCR-004 / SCR-009 / SCR-005. 주행 중에는 화면 이동이 없어 Effect를 쓰지 않는다.
 * ViewModel 생성과 지도 키 확인은 앱 모듈이 맡는다(ADR-009).
 * @param naverMapAvailable 네이버 지도 Client ID가 있는지. 없으면 간이 지도를 그린다(ADR-004).
 */
@Composable
fun DrivingRoute(viewModel: DrivingViewModel, naverMapAvailable: Boolean) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    DisposableEffect(viewModel) {
        viewModel.onIntent(DrivingIntent.Enter)
        onDispose { viewModel.onIntent(DrivingIntent.Exit) }
    }
    DrivingScreen(state = state, onIntent = viewModel::onIntent, naverMapAvailable = naverMapAvailable)
}
