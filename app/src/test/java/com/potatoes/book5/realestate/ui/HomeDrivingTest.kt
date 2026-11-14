package com.potatoes.book5.realestate.ui

import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.DrivingState
import com.potatoes.book5.realestate.domain.model.PriceChange
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.domain.usecase.ObserveInterestConditionUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveNearbyRankingUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrapSummaryUseCase
import com.potatoes.book5.realestate.fakes.FakeConsentRepository
import com.potatoes.book5.realestate.fakes.FakeInterestRepository
import com.potatoes.book5.realestate.fakes.FakeRankingRepository
import com.potatoes.book5.realestate.fakes.FakeScrapRepository
import com.potatoes.book5.realestate.fakes.scrapOf
import com.potatoes.book5.realestate.fakes.snapshotOf
import com.potatoes.book5.realestate.ui.home.HomeEffect
import com.potatoes.book5.realestate.ui.home.HomeIntent
import com.potatoes.book5.realestate.ui.home.HomeViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeDrivingTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

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
            ObserveInterestConditionUseCase(FakeInterestRepository()),
        )
        runCurrent()
        assertEquals(listOf(1, 2, 3), vm.state.value.ranking.map { it.rank })
        assertEquals("a", vm.state.value.leader?.id)
        assertEquals(3, vm.state.value.scrap?.totalCount)
        assertEquals(2, vm.state.value.scrap?.changedCount)
        assertEquals("단지-z", vm.state.value.scrap?.firstChangedName)

        // 관심 조건 카드와 마이 페이지 버튼은 모두 마이 페이지로 간다(SCR-010 진입 경로).
        vm.onIntent(HomeIntent.OpenInterest)
        assertEquals(HomeEffect.Navigate(Screen.MyPage), vm.effect.first())
        vm.onIntent(HomeIntent.OpenMyPage)
        assertEquals(HomeEffect.Navigate(Screen.MyPage), vm.effect.first())
        vm.onIntent(HomeIntent.OpenApartment("a"))
        assertEquals(HomeEffect.Navigate(Screen.Detail("a")), vm.effect.first())
        // 변동이 있으면 '변동 있음' 필터로 연다.
        vm.onIntent(HomeIntent.OpenScrapChanges)
        assertEquals(HomeEffect.Navigate(Screen.Scraps(changedOnly = true)), vm.effect.first())
        vm.onIntent(HomeIntent.OpenScraps)
        assertEquals(HomeEffect.Navigate(Screen.Scraps()), vm.effect.first())
        vm.onIntent(HomeIntent.OpenSettings)
        assertEquals(HomeEffect.Navigate(Screen.Settings), vm.effect.first())
        vm.onIntent(HomeIntent.OpenDriveSummary)
        assertEquals(HomeEffect.Navigate(Screen.DriveSummary()), vm.effect.first())
    }
}
