package com.potatoes.book5.realestate.ui

import com.potatoes.book5.realestate.domain.model.DriveRank
import com.potatoes.book5.realestate.domain.model.PassedInterest
import com.potatoes.book5.realestate.domain.model.OnboardingStep
import com.potatoes.book5.realestate.domain.usecase.MarkDriveSummarySeenUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveDriveSummaryUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrappedIdsUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapByIdUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapUseCase
import com.potatoes.book5.realestate.fakes.FakeApartmentRepository
import com.potatoes.book5.realestate.fakes.FakeDriveRecordRepository
import com.potatoes.book5.realestate.fakes.FakeScrapRepository
import com.potatoes.book5.realestate.fakes.dealOf
import com.potatoes.book5.realestate.ui.summary.DriveSummaryEffect
import com.potatoes.book5.realestate.ui.summary.DriveSummaryIntent
import com.potatoes.book5.realestate.ui.summary.DriveSummaryViewModel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DriveSummaryTest {
    private val dispatcher = StandardTestDispatcher()
    private val t = LocalDateTime.of(2026, 11, 14, 10, 0)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun FakeDriveRecordRepository.finished(hour: Long): Long {
        val id = startSession(t.plusHours(hour), 10)
        recordSnapshot(id, t.plusHours(hour), listOf(DriveRank(1, "a", "단지-a", 1)), listOf(PassedInterest("h", "단지-h", 1.2, 150_000)))
        finishSession(id, null)
        return id
    }

    private fun vm(id: Long?, drives: FakeDriveRecordRepository, scraps: FakeScrapRepository, apartments: FakeApartmentRepository = FakeApartmentRepository()) =
        DriveSummaryViewModel(
            id,
            ObserveDriveSummaryUseCase(drives),
            ObserveScrappedIdsUseCase(scraps),
            MarkDriveSummarySeenUseCase(drives),
            ToggleScrapByIdUseCase(apartments, ToggleScrapUseCase(scraps)),
        )

    @Test
    fun `요약을 열면 확인 처리하고 지나친 관심 매물을 바로 스크랩한다`() = runTest(dispatcher) {
        val drives = FakeDriveRecordRepository()
        val id = drives.finished(0)
        val scraps = FakeScrapRepository()
        val vm = vm(id, drives, scraps, FakeApartmentRepository(mutableMapOf("h" to dealOf(150_000))))
        runCurrent()
        assertEquals(id, vm.state.value.summary?.id)
        assertTrue(drives.sessions.value[id]!!.seen)

        vm.onIntent(DriveSummaryIntent.ToggleScrap("h"))
        runCurrent()
        assertEquals(setOf("h"), vm.state.value.scrappedIds)
        assertEquals(150_000L, scraps.get("h")!!.baseline.priceManwon)

        vm.onIntent(DriveSummaryIntent.OpenScraps)
        assertEquals(DriveSummaryEffect.Navigate(Screen.Scraps()), vm.effect.first())
    }

    @Test
    fun `스크랩 실패는 안내하고 id가 없으면 가장 최근 요약을 연다`() = runTest(dispatcher) {
        val drives = FakeDriveRecordRepository()
        drives.finished(0)
        val latest = drives.finished(2)
        val vm = vm(null, drives, FakeScrapRepository())
        runCurrent()
        assertEquals(latest, vm.state.value.summary?.id)
        vm.onIntent(DriveSummaryIntent.ToggleScrap("unknown"))
        runCurrent()
        assertEquals(DriveSummaryEffect.ShowScrapFailed, vm.effect.first())
        assertFalse("unknown" in vm.state.value.togglingIds)
    }

    @Test
    fun `기록이 없으면 빈 상태다`() = runTest(dispatcher) {
        val vm = vm(null, FakeDriveRecordRepository(), FakeScrapRepository())
        runCurrent()
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.summary)
    }

    @Test
    fun `새 요약은 온보딩이 끝난 뒤에만 한 번 쌓는다`() {
        val onboarding = AppState(stack = listOf(Screen.Interest))
        assertEquals(onboarding, reduceApp(onboarding, AppResult.SummaryReady(1)))
        var state = reduceApp(AppState(), AppResult.StartAt(OnboardingStep.DONE))
        state = reduceApp(state, AppResult.SummaryReady(1))
        assertEquals(Screen.DriveSummary(1), state.current)
        assertEquals(state, reduceApp(state, AppResult.SummaryReady(2)))
    }
}
