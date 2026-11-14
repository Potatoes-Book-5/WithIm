package com.potatoes.book5.realestate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.potatoes.book5.realestate.ui.common.collectAsStateWithAppLifecycle
import com.potatoes.book5.realestate.ui.consent.ConsentRoute
import com.potatoes.book5.realestate.ui.home.HomePlaceholderScreen
import com.potatoes.book5.realestate.ui.interest.InterestRoute

@Composable
fun AppRoot(viewModel: AppViewModel = viewModel(factory = AppViewModels.Factory)) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    BackHandler(enabled = state.interestOpenedFromHome) { viewModel.onIntent(AppIntent.Back) }
    when (state.destination) {
        Destination.LOADING -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        Destination.CONSENT -> ConsentRoute(
            onOpenInterestConditions = { viewModel.onIntent(AppIntent.ConsentFinishedWithLocation) },
            onOpenHome = { viewModel.onIntent(AppIntent.OnboardingFinished) },
        )
        Destination.INTEREST -> InterestRoute(onOpenHome = { viewModel.onIntent(AppIntent.OnboardingFinished) })
        Destination.HOME -> HomePlaceholderScreen(
            onEditConditions = { viewModel.onIntent(AppIntent.OpenInterestFromHome) },
        )
    }
}
