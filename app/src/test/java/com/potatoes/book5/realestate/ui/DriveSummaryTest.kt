package com.potatoes.book5.realestate.ui

import com.potatoes.book5.realestate.domain.model.DriveRank
import com.potatoes.book5.realestate.domain.model.PassedInterest
import com.potatoes.book5.realestate.domain.model.OnboardingStep
import com.potatoes.book5.realestate.domain.model.DriveRecommendation
import com.potatoes.book5.realestate.domain.model.RecommendationKey
import com.potatoes.book5.realestate.domain.usecase.MarkDriveSummarySeenUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveDriveSummaryUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveScrappedIdsUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapByIdUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapUseCase
import com.potatoes.book5.realestate.domain.usecase.RecordRecommendationDetailOpenUseCase
import com.potatoes.book5.realestate.fakes.FakeApartmentRepository
import com.potatoes.book5.realestate.fakes.FakeDriveRecordRepository
import com.potatoes.book5.realestate.fakes.FakeScrapRepository
import com.potatoes.book5.realestate.fakes.FakeRecommendationFeedbackRepository
import com.potatoes.book5.realestate.fakes.dealOf
import com.potatoes.book5.realestate.ui.summary.DriveSummaryEffect
import com.potatoes.book5.realestate.ui.summary.DriveSummaryIntent
import com.potatoes.book5.realestate.ui.summary.DriveSummaryViewModel
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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

    private fun vm(
        id: Long?,
        drives: FakeDriveRecordRepository,
        scraps: FakeScrapRepository,
        apartments: FakeApartmentRepository = FakeApartmentRepository(),
        feedback: FakeRecommendationFeedbackRepository = FakeRecommendationFeedbackRepository(),
    ) =
        DriveSummaryViewModel(
            id,
            ObserveDriveSummaryUseCase(drives),
            ObserveScrappedIdsUseCase(scraps),
            MarkDriveSummarySeenUseCase(drives),
            ToggleScrapByIdUseCase(apartments, ToggleScrapUseCase(scraps)),
            RecordRecommendationDetailOpenUseCase(feedback),
        )

    @Test
    fun `추천 카드 상세 진입 반응을 기록하고 상세를 연다`() = runTest(dispatcher) {
        val drives = FakeDriveRecordRepository()
        val id = drives.finished(0)
        val feedback = FakeRecommendationFeedbackRepository()
        val recommendation = DriveRecommendation("a", "추천", 100_000, 30, 88, 1.0, RecommendationKey.PRICE)
        val vm = vm(id, drives, FakeScrapRepository(), feedback = feedback)
        val effects = mutableListOf<DriveSummaryEffect>()
        val job = backgroundScope.launch { vm.effect.collect(effects::add) }

        vm.onIntent(DriveSummaryIntent.OpenRecommendation(recommendation))
        runCurrent()

        assertEquals(1, feedback.getAll().single().detailOpenCount)
        assertEquals(DriveSummaryEffect.Navigate(Screen.Detail("a")), effects.single())
        job.cancel()
    }

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
    fun `요약은 추천 상위 세 개만 계산하고 전체 추천과 추가 관심 매물은 보존한다`() = runTest(dispatcher) {
        val drives = FakeDriveRecordRepository()
        val id = drives.startSession(t, 10)
        drives.recordSnapshot(
            sessionId = id,
            at = t,
            top = emptyList(),
            passed = listOf(
                PassedInterest("featured", "추천 단지", 1.0, 120_000),
                PassedInterest("legacy", "기존 관심 단지", 2.5, 90_000),
            ),
            recommendations = listOf(
                DriveRecommendation("featured", "추천 단지", 120_000, 30, 95, 1.0, RecommendationKey.PRICE),
                DriveRecommendation("second", "다른 추천", 110_000, 28, 90, 1.5, RecommendationKey.AREA),
                DriveRecommendation("third", "세 번째 추천", 100_000, 25, 85, 1.8, RecommendationKey.NEWNESS),
            ),
        )
        drives.recordAlertedRecommendation(
            t.plusMinutes(1), 10,
            DriveRecommendation("outside-top3", "알림 단지", 130_000, 30, 98, 1.9, RecommendationKey.PRICE),
        )
        drives.finishSession(id, null)

        val summary = drives.observeSummary(id).first()!!

        assertEquals(listOf("legacy"), summary.additionalPassedInterests.map { it.apartmentId })
        assertEquals(5, summary.interestSummaryCount)
        assertEquals(listOf("featured", "second", "third", "outside-top3"), summary.recommendations.map { it.apartmentId })
        assertEquals(3, summary.recommendationTop.size)
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

    @Test
    fun `모아보기를 보고 있으면 새 요약을 그 위에 쌓지 않는다`() {
        var state = reduceApp(AppState(), AppResult.StartAt(OnboardingStep.DONE))
        state = reduceApp(state, AppResult.Push(Screen.InterestCatchUp(1)))
        assertEquals(state, reduceApp(state, AppResult.SummaryReady(2)))
    }

    @Test
    fun `주행 요약에서 관심 매물 모아보기를 누르면 같은 주행의 모아보기를 연다`() = runTest(dispatcher) {
        val drives = FakeDriveRecordRepository()
        val id = drives.finished(0)
        val vm = vm(id, drives, FakeScrapRepository())
        runCurrent()
        vm.onIntent(DriveSummaryIntent.OpenInterestCatchUp)
        assertEquals(DriveSummaryEffect.Navigate(Screen.InterestCatchUp(id)), vm.effect.first())
    }
}
