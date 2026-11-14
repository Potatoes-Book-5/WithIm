package com.potatoes.book5.realestate.ui.driving

import androidx.lifecycle.SavedStateHandle
import com.potatoes.book5.realestate.domain.model.Apartment
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.model.RankingSnapshot
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.RankingRepository
import com.potatoes.book5.realestate.domain.usecase.ObserveLeaderChangesUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveNearbyRankingUseCase
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

/** 주행 화면(SCR-004, SCR-005, SCR-009) ViewModel과 리듀서. app 모듈의 HomeDrivingTest에서 옮겼다(ADR-009). */
@OptIn(ExperimentalCoroutinesApi::class)
class DrivingViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val consent = object : ConsentRepository {
        override suspend fun get() = ConsentSettings(locationConsented = true, interestAlertEnabled = true, leaderAlertEnabled = true)
        override suspend fun save(settings: ConsentSettings) = Unit
    }

    private class FakeRankingRepository(initial: RankingSnapshot) : RankingRepository {
        val snapshots = MutableStateFlow(initial)
        override fun observeNearbyRanking(): Flow<RankingSnapshot> = snapshots
    }

    /** leaderIds 순서대로 1위부터 채운 스냅샷. */
    private fun snapshotOf(vararg leaderIds: String) = RankingSnapshot(
        neighborhoodName = "정자동",
        radiusKm = 10,
        top = leaderIds.mapIndexed { i, id ->
            NearbyApartment(Apartment(id, "단지-$id", 200_000L - i * 10_000, 34, LocalDate.of(2026, 10, 1)), 1.0 + i, 0.0)
        },
        interestMatches = emptyList(),
        others = emptyList(),
        dataAsOf = LocalDateTime.of(2026, 11, 13, 9, 0),
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
