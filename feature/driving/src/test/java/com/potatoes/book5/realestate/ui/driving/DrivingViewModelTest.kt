package com.potatoes.book5.realestate.ui.driving

import androidx.lifecycle.SavedStateHandle
import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.Apartment
import com.potatoes.book5.realestate.domain.model.ApartmentDetail
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.Deal
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.RankingSnapshot
import com.potatoes.book5.realestate.domain.model.Scrap
import com.potatoes.book5.realestate.domain.port.ApartmentRepository
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.MarketRepository
import com.potatoes.book5.realestate.domain.port.RankingRepository
import com.potatoes.book5.realestate.domain.usecase.ObserveLeaderChangesUseCase
import com.potatoes.book5.realestate.domain.usecase.ObserveNearbyRankingUseCase
import com.potatoes.book5.realestate.domain.usecase.RefreshMarketUseCase
import com.potatoes.book5.realestate.domain.port.ScrapRepository
import com.potatoes.book5.realestate.domain.usecase.ObserveInterestMatchAlertsUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapByIdUseCase
import com.potatoes.book5.realestate.domain.usecase.ToggleScrapUseCase
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 주행 화면(SCR-004, SCR-005, SCR-009) ViewModel과 리듀서. app 모듈의 HomeDrivingTest에서 옮겼다(ADR-009). */
@OptIn(ExperimentalCoroutinesApi::class)
class DrivingViewModelTest {

    private fun emptyMarketRefresh() = RefreshMarketUseCase(object : MarketRepository {
        override fun observeNearbyRanking(): Flow<RankingSnapshot> = MutableStateFlow(snapshotOf())
        override suspend fun detail(apartmentId: String): Outcome<ApartmentDetail> =
            Outcome.Failure(DomainError.Invalid("unknown-apartment"))
        override suspend fun latestDeal(apartmentId: String): Outcome<Deal> =
            Outcome.Failure(DomainError.Invalid("unknown-apartment"))
        override suspend fun suggestions(): Outcome<List<Neighborhood>> = Outcome.Success(emptyList())
        override suspend fun refresh(): Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun run() = Unit
    })

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

    private class FakeScrapRepository : ScrapRepository {
        val scraps = MutableStateFlow<Map<String, Scrap>>(emptyMap())
        override fun observeAll(): Flow<List<Scrap>> = scraps.map { it.values.toList() }
        override fun observeIsScrapped(apartmentId: String): Flow<Boolean> = scraps.map { apartmentId in it }
        override suspend fun getAll(): List<Scrap> = scraps.value.values.toList()
        override suspend fun get(apartmentId: String): Scrap? = scraps.value[apartmentId]
        override suspend fun upsert(scrap: Scrap) = scraps.update { it + (scrap.apartmentId to scrap) }
        override suspend fun delete(apartmentId: String) = scraps.update { it - apartmentId }
        override suspend fun markChangeSeen(apartmentId: String) = Unit
        override suspend fun deleteAll() = scraps.update { emptyMap() }
    }

    private val apartments = object : ApartmentRepository {
        private val deal = Deal(LocalDate.of(2026, 10, 1), 34, 10, 158_000)
        override suspend fun detail(apartmentId: String) = Outcome.Success(
            ApartmentDetail(
                id = apartmentId, name = "단지-$apartmentId", district = "분당구", neighborhood = "정자동", households = 1_000,
                builtYear = 2010, minPyeong = 24, maxPyeong = 45, distanceKm = 1.0, latestDeal = deal, previousDeal = null,
                trend = emptyList(), recentDeals = listOf(deal),
            )
        )
        override suspend fun latestDeal(apartmentId: String) = Outcome.Success(deal)
    }

    private fun viewModel(ranking: RankingRepository, scraps: ScrapRepository = FakeScrapRepository(), handle: SavedStateHandle = SavedStateHandle()) =
        DrivingViewModel(
            handle,
            ObserveNearbyRankingUseCase(ranking),
            ObserveLeaderChangesUseCase(ranking, consent, Duration.ZERO),
            emptyMarketRefresh(),
            ObserveInterestMatchAlertsUseCase(ranking, consent, scraps),
            ToggleScrapByIdUseCase(apartments, ToggleScrapUseCase(scraps)),
        )

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
    fun `주행 화면은 대장 교체 알림을 띄우고 10초 뒤 닫는다`() = runTest(dispatcher) {
        val ranking = FakeRankingRepository(snapshotOf("a", "b"))
        val vm = viewModel(ranking)
        runCurrent()
        assertNull(vm.state.value.alert)

        ranking.snapshots.value = snapshotOf("b", "a")
        runCurrent()
        val alert = vm.state.value.alert as? LeaderAlertUi
        assertNotNull(alert)
        assertEquals("단지-a", alert!!.previousName)
        assertEquals("단지-b", alert.currentName)

        advanceTimeBy(DrivingViewModel.LEADER_ALERT_DURATION_MILLIS - 1)
        assertNotNull(vm.state.value.alert)
        advanceTimeBy(2)
        assertNull(vm.state.value.alert)
    }

    @Test
    fun `순위와 지도 보기를 바꾸고 기억한다`() = runTest(dispatcher) {
        val handle = SavedStateHandle()
        val ranking = FakeRankingRepository(snapshotOf("a"))
        val vm = viewModel(ranking, handle = handle)
        vm.onIntent(DrivingIntent.SelectMode(DrivingMode.MAP))
        assertEquals(DrivingMode.MAP, vm.state.value.mode)

        val restored = viewModel(ranking, handle = handle)
        assertEquals(DrivingMode.MAP, restored.state.value.mode)
    }

    @Test
    fun `관심 매물에 가까워지면 상단 배너를 띄우고 한 번 탭으로 스크랩한다`() = runTest(dispatcher) {
        val ranking = FakeRankingRepository(snapshotOf("a"))
        val scraps = FakeScrapRepository()
        val vm = viewModel(ranking, scraps)
        runCurrent()

        val near = NearbyApartment(Apartment("h", "한빛마을 1단지", 158_000, 34, LocalDate.of(2026, 10, 1)), 1.2, -70.0)
        ranking.snapshots.value = snapshotOf("a").copy(interestMatches = listOf(near))
        runCurrent()
        val alert = vm.state.value.alert as? InterestAlertUi
        assertNotNull(alert)
        assertEquals("한빛마을 1단지", alert!!.name)
        assertEquals(ScrapButtonState.IDLE, alert.scrap)

        vm.onIntent(DrivingIntent.ScrapAlert(alert.key))
        runCurrent()
        assertEquals(ScrapButtonState.SAVED, (vm.state.value.alert as InterestAlertUi).scrap)
        assertTrue("h" in scraps.scraps.value)

        // 다시 눌러도 해제하지 않는다.
        vm.onIntent(DrivingIntent.ScrapAlert(alert.key))
        runCurrent()
        assertTrue("h" in scraps.scraps.value)

        advanceTimeBy(DrivingViewModel.INTEREST_ALERT_DURATION_MILLIS + 1)
        assertNull(vm.state.value.alert)
    }

    @Test
    fun `배너가 닫힌 뒤의 스크랩 결과는 상태를 바꾸지 않는다`() {
        val banner = InterestAlertUi(1, "h", "한빛마을 1단지", 158_000, 34, 1.2, 0.0)
        val leader = LeaderAlertUi(2, "a", "b", 100_000, 1.0)
        var state = reduceDriving(DrivingScreenState(), DrivingResult.AlertShown(banner))
        state = reduceDriving(state, DrivingResult.AlertShown(leader))
        assertEquals(leader, reduceDriving(state, DrivingResult.AlertScrapChanged(1, ScrapButtonState.SAVED)).alert)
    }
}
