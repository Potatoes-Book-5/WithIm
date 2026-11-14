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
import com.potatoes.book5.realestate.ui.detail.DetailRoute
import com.potatoes.book5.realestate.ui.dev.DevControls
import com.potatoes.book5.realestate.ui.driving.DrivingRoute
import com.potatoes.book5.realestate.ui.home.HomeRoute
import com.potatoes.book5.realestate.ui.interest.InterestRoute
import com.potatoes.book5.realestate.ui.mypage.MyPageRoute
import com.potatoes.book5.realestate.ui.scrap.ScrapListRoute

@Composable
fun AppRoot(viewModel: AppViewModel = viewModel(factory = AppViewModels.Factory)) {
    val state by viewModel.state.collectAsStateWithAppLifecycle()
    val onBack = { viewModel.onIntent(AppIntent.Back) }
    val open: (Screen) -> Unit = { viewModel.onIntent(AppIntent.Open(it)) }
    // 주행 중에는 화면 이동을 막고, 주차 중에만 스택을 되돌린다.
    BackHandler(enabled = state.canGoBack && !state.showDriving, onBack = onBack)
    val stores = rememberScreenStores(state.stack)
    val currentKey = screenKey(state.stack.lastIndex, state.current)
    Box(Modifier.fillMaxSize()) {
        if (state.showDriving) {
            DrivingRoute()
        } else {
            when (val screen = state.current) {
                Screen.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                Screen.Consent -> ConsentRoute(
                    onOpenInterestConditions = { viewModel.onIntent(AppIntent.ConsentFinishedWithLocation) },
                    onOpenHome = { viewModel.onIntent(AppIntent.OnboardingFinished) },
                )
                Screen.Interest -> if (state.onboardingDone) {
                    // 온보딩 뒤 조건 수정: 저장하거나 취소하면 연 화면으로 돌아간다.
                    ProvideScreenStore(stores, currentKey) { InterestRoute(onOpenHome = onBack, editing = true) }
                } else {
                    InterestRoute(onOpenHome = { viewModel.onIntent(AppIntent.OnboardingFinished) })
                }
                Screen.Home -> HomeRoute(onOpen = open)
                is Screen.Scraps -> ProvideScreenStore(stores, currentKey) {
                    ScrapListRoute(
                        changedOnly = screen.changedOnly,
                        onOpenDetail = { open(Screen.Detail(it)) },
                        onBack = onBack,
                    )
                }
                is Screen.Detail -> ProvideScreenStore(stores, currentKey) {
                    DetailRoute(apartmentId = screen.apartmentId, onBack = onBack)
                }
                Screen.MyPage -> ProvideScreenStore(stores, currentKey) {
                    MyPageRoute(onOpen = open, onBack = onBack)
                }
            }
        }
        // 실제 주행 상태 연동 전까지 개발 빌드에서만 주행과 시세 변동을 흉내 낸다.
        if (BuildConfig.DEBUG && state.onboardingDone) {
            DevControls(
                driving = state.showDriving,
                onSetDriving = { viewModel.onIntent(AppIntent.DevSetDriving(it)) },
                onNextArea = { viewModel.onIntent(AppIntent.DevMoveToNextArea) },
                onPublishNewDeals = { viewModel.onIntent(AppIntent.DevPublishNewDeals) },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}
