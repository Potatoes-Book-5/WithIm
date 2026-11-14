package com.potatoes.book5.realestate.ui.driving

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.potatoes.book5.realestate.ui.AppViewModels
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle

/** SCR-004 / SCR-009 / SCR-005. 주행 중에는 화면 이동이 없어 Effect를 쓰지 않는다. */
@Composable
fun DrivingRoute(viewModel: DrivingViewModel = viewModel(factory = AppViewModels.Factory)) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    DrivingScreen(state = state, onIntent = viewModel::onIntent)
}
