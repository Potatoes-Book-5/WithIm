package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.model.ConsentSettings
import com.potatoes.book5.realestate.domain.model.LeaderChange
import com.potatoes.book5.realestate.fakes.FakeConsentRepository
import com.potatoes.book5.realestate.fakes.FakeRankingRepository
import com.potatoes.book5.realestate.fakes.snapshotOf
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderChangesUseCaseTest {

    private fun consent(leaderAlert: Boolean) = FakeConsentRepository(
        ConsentSettings(locationConsented = true, interestAlertEnabled = true, leaderAlertEnabled = leaderAlert)
    )

    private class Clock(var now: LocalDateTime = LocalDateTime.of(2026, 11, 14, 10, 0))

    @Test
    fun `처음 받은 순위는 교체로 보지 않고 1위가 바뀌면 알린다`() = runTest {
        val ranking = FakeRankingRepository(snapshotOf("a", "b"))
        val clock = Clock()
        val events = mutableListOf<LeaderChange>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveLeaderChangesUseCase(ranking, consent(true), now = { clock.now })().collect { events += it }
        }
        assertTrue(events.isEmpty())

        ranking.snapshots.value = snapshotOf("b", "a")
        assertEquals(1, events.size)
        assertEquals("a", events[0].previous.id)
        assertEquals("b", events[0].current.apartment.id)
        job.cancel()
    }

    @Test
    fun `1위가 그대로면 순서가 바뀌어도 알리지 않는다`() = runTest {
        val ranking = FakeRankingRepository(snapshotOf("a", "b", "c"))
        val events = mutableListOf<LeaderChange>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveLeaderChangesUseCase(ranking, consent(true))().collect { events += it }
        }
        ranking.snapshots.value = snapshotOf("a", "c", "b")
        assertTrue(events.isEmpty())
        job.cancel()
    }

    @Test
    fun `마지막 알림 후 최소 간격 안의 교체는 알리지 않는다`() = runTest {
        val ranking = FakeRankingRepository(snapshotOf("a"))
        val clock = Clock()
        val events = mutableListOf<LeaderChange>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveLeaderChangesUseCase(ranking, consent(true), Duration.ofMinutes(5), now = { clock.now })().collect { events += it }
        }
        ranking.snapshots.value = snapshotOf("b")
        clock.now = clock.now.plusMinutes(2)
        ranking.snapshots.value = snapshotOf("c")
        assertEquals(1, events.size)

        clock.now = clock.now.plusMinutes(4)
        ranking.snapshots.value = snapshotOf("d")
        assertEquals(2, events.size)
        // 억제된 교체도 기록되므로 직전 대장은 c다.
        assertEquals("c", events[1].previous.id)
        job.cancel()
    }

    @Test
    fun `대장 교체 알림을 끄면 알리지 않는다`() = runTest {
        val ranking = FakeRankingRepository(snapshotOf("a"))
        val events = mutableListOf<LeaderChange>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            ObserveLeaderChangesUseCase(ranking, consent(false))().collect { events += it }
        }
        ranking.snapshots.value = snapshotOf("b")
        assertTrue(events.isEmpty())
        job.cancel()
    }
}
