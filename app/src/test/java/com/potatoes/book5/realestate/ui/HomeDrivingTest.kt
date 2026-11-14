package com.potatoes.book5.realestate.ui

import androidx.lifecycle.SavedStateHandle
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.DrivingState
import com.potatoes.book5.realestate.domain.model.PriceChange
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.domain.usecase.GetInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveLeaderChangesUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveNearbyRankingUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrapSummaryUseCase
import com.potatoes.book5.realestate.fakes.FakeConsentRepository
import com.potatoes.book5.realestate.fakes.FakeInterestRepository
import com.potatoes.book5.realestate.fakes.FakeRankingRepository
import com.potatoes.book5.realestate.fakes.FakeScrapRepository
import com.potatoes.book5.realestate.fakes.scrapOf
import com.potatoes.book5.realestate.fakes.snapshotOf
import com.potatoes.book5.realestate.ui.driving.DrivingIntent
import com.potatoes.book5.realestate.ui.driving.DrivingMode
import com.potatoes.book5.realestate.ui.driving.DrivingResult
import com.potatoes.book5.realestate.ui.driving.DrivingScreenState
import com.potatoes.book5.realestate.ui.driving.DrivingViewModel
import com.potatoes.book5.realestate.ui.driving.LeaderAlertUi
import com.potatoes.book5.realestate.ui.driving.reduceDriving
import com.potatoes.book5.realestate.ui.home.HomeEffect
import com.potatoes.book5.realestate.ui.home.HomeIntent
import com.potatoes.book5.realestate.ui.home.HomeViewModel
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDrivingTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val consent = FakeConsentRepository(
        ConsentSettings(locationConsented = true, interestAlertEnabled = true, leaderAlertEnabled = true)
    )

    @Test
    fun `알림이 겹치면 이전 알림의 만료가 새 알림을 지우지 않는다`() {
        val first = LeaderAlertUi(1, "a", "b", 100_000, 1.0)
        val second = LeaderAlertUi(2, "b", "c", 100_000, 1.0)
        var state = reduceDriving(DrivingScreenState(), DrivingResult.AlertShown(first))
        state = reduceDriving(state, DrivingResult.AlertShown(second))
        state = reduceDriving(state, DrivingResult.AlertExpired(1))
        assertEquals(second, state.alert)
        assertNull(reduceDriving(state, DrivingResult.AlertExpired(2)).alert)
    }

    @Test
    fun `앱 상태는 홈에서 주행 중일 때만 주행 화면을 보여 준다`() {
        val home = AppState(stack = listOf(Screen.Home))
        assertEquals(false, home.showDriving)
        assertEquals(true, reduceApp(home, AppResult.DrivingChanged(DrivingState.DRIVING)).showDriving)
        // 홈에서 연 상세에 있어도 주행 화면으로 바뀐다.
        val detail = AppState(stack = listOf(Screen.Home, Screen.Detail("a")), drivingState = DrivingState.DRIVING)
        assertEquals(true, detail.showDriving)
        val onboarding = AppState(stack = listOf(Screen.Interest), drivingState = DrivingState.DRIVING)
        assertEquals(false, onboarding.showDriving)
    }

    @Test
    fun `홈은 순위와 스크랩 요약을 불러오고 준비 중인 화면은 안내한다`() = runTest(dispatcher) {
        val ranking = FakeRankingRepository(snapshotOf("a", "b", "c"))
        val vm = HomeViewModel(
            ObserveNearbyRankingUseCase(ranking),
            ObserveScrapSummaryUseCase(
                FakeScrapRepository(
                    listOf(
                        scrapOf("x"),
                        scrapOf("y", change = PriceChange(ScrapChangeKind.NEW_DEAL, 3_000, LocalDateTime.of(2026, 11, 1, 9, 0), seen = false)),
                        scrapOf("z", change = PriceChange(ScrapChangeKind.PRICE_UP, 1_000, LocalDateTime.of(2026, 11, 2, 9, 0), seen = false)),
                    )
                )
            ),
            GetInterestConditionUseCase(FakeInterestRepository()),
        )
        runCurrent()
        assertEquals(listOf(1, 2, 3), vm.state.value.ranking.map { it.rank })
        assertEquals("a", vm.state.value.leader?.id)
        assertEquals(3, vm.state.value.scrap?.totalCount)
        assertEquals(2, vm.state.value.scrap?.changedCount)
        assertEquals("단지-z", vm.state.value.scrap?.firstChangedName)

        vm.onIntent(HomeIntent.OpenInterest)
        assertEquals(HomeEffect.Navigate(Screen.Interest), vm.effect.first())
        vm.onIntent(HomeIntent.OpenApartment("a"))
        assertEquals(HomeEffect.Navigate(Screen.Detail("a")), vm.effect.first())
        // 변동이 있으면 '변동 있음' 필터로 연다.
        vm.onIntent(HomeIntent.OpenScrapChanges)
        assertEquals(HomeEffect.Navigate(Screen.Scraps(changedOnly = true)), vm.effect.first())
        vm.onIntent(HomeIntent.OpenScraps)
        assertEquals(HomeEffect.Navigate(Screen.Scraps()), vm.effect.first())
    }

    @Test
    fun `주행 화면은 대장 교체 알림을 띄우고 8초 뒤 닫는다`() = runTest(dispatcher) {
        val ranking = FakeRankingRepository(snapshotOf("a", "b"))
        val vm = DrivingViewModel(
            SavedStateHandle(),
            ObserveNearbyRankingUseCase(ranking),
            ObserveLeaderChangesUseCase(ranking, consent, Duration.ZERO),
        )
        runCurrent()
        assertNull(vm.state.value.alert)

        ranking.snapshots.value = snapshotOf("b", "a")
        runCurrent()
        val alert = vm.state.value.alert
        assertNotNull(alert)
        assertEquals("단지-a", alert!!.previousName)
        assertEquals("단지-b", alert.currentName)

        advanceTimeBy(DrivingViewModel.ALERT_DURATION_MILLIS - 1)
        assertNotNull(vm.state.value.alert)
        advanceTimeBy(2)
        assertNull(vm.state.value.alert)
    }

    @Test
    fun `순위와 지도 보기를 바꾸고 기억한다`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val ranking = FakeRankingRepository(snapshotOf("a"))
        val vm = DrivingViewModel(handle, ObserveNearbyRankingUseCase(ranking), ObserveLeaderChangesUseCase(ranking, consent))
        vm.onIntent(DrivingIntent.SelectMode(DrivingMode.MAP))
        assertEquals(DrivingMode.MAP, vm.state.value.mode)

        val restored = DrivingViewModel(handle, ObserveNearbyRankingUseCase(ranking), ObserveLeaderChangesUseCase(ranking, consent))
        assertEquals(DrivingMode.MAP, restored.state.value.mode)
    }
}
