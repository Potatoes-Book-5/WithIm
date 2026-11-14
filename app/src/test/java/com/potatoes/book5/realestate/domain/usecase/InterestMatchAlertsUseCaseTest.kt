package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.model.Apartment
import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.fakes.FakeConsentRepository
import com.potatoes.book5.realestate.fakes.FakeRankingRepository
import com.potatoes.book5.realestate.fakes.FakeScrapRepository
import com.potatoes.book5.realestate.fakes.scrapOf
import com.potatoes.book5.realestate.fakes.snapshotOf
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 관심 매물 발견 알림(F-005 규칙 2~4, F-006 규칙 5·7, T-025). */
@OptIn(ExperimentalCoroutinesApi::class)
class InterestMatchAlertsUseCaseTest {

    private fun consent(enabled: Boolean) = FakeConsentRepository(
        ConsentSettings(locationConsented = true, interestAlertEnabled = enabled, leaderAlertEnabled = true)
    )

    private fun match(id: String, km: Double) =
        NearbyApartment(Apartment(id, "단지-$id", 150_000, 34, LocalDate.of(2026, 10, 1)), km, 30.0)

    private fun snapshotWith(vararg matches: NearbyApartment) = snapshotOf("a").copy(interestMatches = matches.toList())

    private class Clock(var now: LocalDateTime = LocalDateTime.of(2026, 11, 14, 10, 0))

    private fun collect(
        useCase: ObserveInterestMatchAlertsUseCase,
        scope: kotlinx.coroutines.test.TestScope,
        into: MutableList<InterestMatchAlert>,
    ) = scope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { useCase().collect { into += it } }

    @Test
    fun `알림 거리 안의 관심 매물 중 가장 가까운 1건만 알린다`() = runTest {
        val ranking = FakeRankingRepository(snapshotWith(match("far", 3.5), match("b", 1.8), match("c", 0.9)))
        val events = mutableListOf<InterestMatchAlert>()
        val job = collect(ObserveInterestMatchAlertsUseCase(ranking, consent(true), FakeScrapRepository()), this, events)
        assertEquals(listOf("c"), events.map { it.match.apartment.id })
        job.cancel()
    }

    @Test
    fun `같은 단지는 억제 기간 안에 다시 알리지 않고 다음 후보를 알린다`() = runTest {
        val ranking = FakeRankingRepository(snapshotWith(match("b", 1.8), match("c", 0.9)))
        val clock = Clock()
        val events = mutableListOf<InterestMatchAlert>()
        val useCase = ObserveInterestMatchAlertsUseCase(ranking, consent(true), FakeScrapRepository(), now = { clock.now })
        val job = collect(useCase, this, events)

        clock.now = clock.now.plusMinutes(1)
        ranking.snapshots.value = snapshotWith(match("b", 1.5), match("c", 0.7))
        clock.now = clock.now.plusMinutes(1)
        ranking.snapshots.value = snapshotWith(match("b", 1.2), match("c", 0.5))
        assertEquals(listOf("c", "b"), events.map { it.match.apartment.id })

        clock.now = clock.now.plusHours(24)
        ranking.snapshots.value = snapshotWith(match("c", 0.4))
        assertEquals(listOf("c", "b", "c"), events.map { it.match.apartment.id })
        job.cancel()
    }

    @Test
    fun `스크랩한 단지는 알리지 않는다`() = runTest {
        val ranking = FakeRankingRepository(snapshotWith(match("c", 0.9)))
        val events = mutableListOf<InterestMatchAlert>()
        val job = collect(ObserveInterestMatchAlertsUseCase(ranking, consent(true), FakeScrapRepository(listOf(scrapOf("c")))), this, events)
        assertTrue(events.isEmpty())
        job.cancel()
    }

    @Test
    fun `관심 매물 알림이 꺼져 있으면 알리지 않는다`() = runTest {
        val ranking = FakeRankingRepository(snapshotWith(match("c", 0.9)))
        val events = mutableListOf<InterestMatchAlert>()
        val job = collect(ObserveInterestMatchAlertsUseCase(ranking, consent(false), FakeScrapRepository()), this, events)
        assertTrue(events.isEmpty())
        job.cancel()
    }
}
