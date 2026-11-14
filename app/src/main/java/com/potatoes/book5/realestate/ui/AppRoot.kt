package com.potatoes.book5.realestate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.potatoes.book5.realestate.BuildConfig
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle
import com.potatoes.book5.realestate.ui.consent.ConsentRoute
import com.potatoes.book5.realestate.ui.dev.DevControls
import com.potatoes.book5.realestate.ui.driving.DrivingRoute
import com.potatoes.book5.realestate.ui.home.HomeRoute
import com.potatoes.book5.realestate.ui.interest.InterestRoute

@Composable
fun AppRoot(viewModel: AppViewModel = viewModel(factory = AppViewModels.Factory)) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    BackHandler(enabled = state.interestOpenedFromHome) { viewModel.onIntent(AppIntent.Back) }
    Box(Modifier.fillMaxSize()) {
        when (state.destination) {
            Destination.LOADING -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            Destination.CONSENT -> ConsentRoute(
                onOpenInterestConditions = { viewModel.onIntent(AppIntent.ConsentFinishedWithLocation) },
                onOpenHome = { viewModel.onIntent(AppIntent.OnboardingFinished) },
            )
            Destination.INTEREST -> InterestRoute(onOpenHome = { viewModel.onIntent(AppIntent.OnboardingFinished) })
            Destination.HOME -> if (state.showDriving) {
                DrivingRoute()
            } else {
                HomeRoute(onOpenInterestConditions = { viewModel.onIntent(AppIntent.OpenInterestFromHome) })
            }
        }
        // 실제 주행 상태 연동 전까지 개발 빌드에서만 주행을 흉내 낸다.
        if (BuildConfig.DEBUG && state.destination == Destination.HOME) {
            DevControls(
                driving = state.showDriving,
                onSetDriving = { viewModel.onIntent(AppIntent.DevSetDriving(it)) },
                onNextArea = { viewModel.onIntent(AppIntent.DevMoveToNextArea) },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
