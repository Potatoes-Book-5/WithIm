package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.model.Apartment
import com.potatoes.book5.realestate.domain.model.DrivingState
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.port.DrivingStateProvider
import com.potatoes.book5.realestate.fakes.FakeDriveRecordRepository
import com.potatoes.book5.realestate.fakes.FakeRankingRepository
import com.potatoes.book5.realestate.fakes.snapshotOf
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DriveSessionRecorderTest {

    private val driving = MutableStateFlow(DrivingState.PARKED)
    private val provider = object : DrivingStateProvider {
        override val drivingState = driving
    }
    private val ranking = FakeRankingRepository(snapshotOf("a", "b", "c"))
    private val repo = FakeDriveRecordRepository()

    /** 가상 시간(분)을 시각으로 쓴다. */
    private fun TestScope.recorder() = DriveSessionRecorder(
        provider, ranking, repo,
        now = { LocalDateTime.of(2026, 11, 14, 10, 0).plusSeconds(testScheduler.currentTime / 1000) },
        endDelay = 3.minutes,
        keep = 2,
    )

    private fun session() = repo.sessions.value.values.single()

    @Test
    fun `주행 중 순위와 대장 교체를 기록하고 주차 3분 뒤 주차 시각으로 닫는다`() = runTest {
        val job = launch { recorder().run() }
        runCurrent()
        driving.value = DrivingState.DRIVING
        runCurrent()
        advanceTimeBy(10.minutes.inWholeMilliseconds)
        ranking.snapshots.value = snapshotOf("b", "a", "c")
        runCurrent()
        advanceTimeBy(5.minutes.inWholeMilliseconds)
        ranking.snapshots.value = snapshotOf("c", "b", "a")
        runCurrent()
        assertEquals(listOf("b", "c"), session().changes.map { it.apartmentId })
        assertEquals(listOf("c", "b", "a"), session().top.map { it.apartmentId })

        advanceTimeBy(1.minutes.inWholeMilliseconds)
        driving.value = DrivingState.PARKED
        runCurrent()
        advanceTimeBy(2.minutes.inWholeMilliseconds)
        runCurrent()
        assertNull(session().endedAt)
        advanceTimeBy(1.minutes.inWholeMilliseconds + 1)
        runCurrent()
        // 주차한 시각(16분)으로 닫힌다.
        assertEquals(LocalDateTime.of(2026, 11, 14, 10, 16), session().endedAt)
        job.cancel()
    }

    @Test
    fun `주차 후 3분 안에 다시 달리면 같은 세션을 이어 간다`() = runTest {
        val job = launch { recorder().run() }
        driving.value = DrivingState.DRIVING
        runCurrent()
        driving.value = DrivingState.PARKED
        runCurrent()
        advanceTimeBy(2.minutes.inWholeMilliseconds)
        driving.value = DrivingState.DRIVING
        runCurrent()
        ranking.snapshots.value = snapshotOf("b", "a")
        runCurrent()
        assertEquals(1, repo.sessions.value.size)
        assertNull(session().endedAt)
        assertEquals(1, session().changes.size)
        job.cancel()
    }

    @Test
    fun `종료 신호를 받으면 기다리지 않고 닫고 기록 없는 세션은 지운다`() = runTest {
        val r = recorder()
        val job = launch { r.run() }
        driving.value = DrivingState.DRIVING
        runCurrent()
        driving.value = DrivingState.PARKED
        r.endNow()
        runCurrent()
        assertTrue(session().endedAt != null)

        ranking.snapshots.value = snapshotOf().copy(top = emptyList())
        driving.value = DrivingState.DRIVING
        runCurrent()
        driving.value = DrivingState.PARKED
        r.endNow()
        runCurrent()
        // 순위가 없어 세션을 만들지 않았다.
        assertEquals(1, repo.sessions.value.size)
        job.cancel()
    }

    @Test
    fun `관심 매물은 가장 가까웠던 거리로 모은다`() = runTest {
        val match = { km: Double -> NearbyApartment(Apartment("h", "한빛", 158_000, 34, LocalDate.of(2026, 10, 1)), km, 0.0) }
        val job = launch { recorder().run() }
        ranking.snapshots.value = snapshotOf("a").copy(interestMatches = listOf(match(3.0)))
        driving.value = DrivingState.DRIVING
        runCurrent()
        ranking.snapshots.value = snapshotOf("a").copy(interestMatches = listOf(match(1.2)))
        runCurrent()
        ranking.snapshots.value = snapshotOf("a").copy(interestMatches = listOf(match(4.0)))
        runCurrent()
        assertEquals(1.2, session().passed.single().closestKm, 1e-9)
        job.cancel()
    }

    @Test
    fun `시작할 때 열린 세션은 마지막 기록 시각으로 닫고 최근 요약만 남긴다`() = runTest {
        val t = LocalDateTime.of(2026, 11, 13, 9, 0)
        repeat(3) { i ->
            val id = repo.startSession(t.plusHours(i.toLong()), 10)
            repo.recordSnapshot(id, t.plusHours(i.toLong()).plusMinutes(30), listOf(com.potatoes.book5.realestate.domain.model.DriveRank(1, "a", "a", 1)), emptyList())
            if (i < 2) repo.finishSession(id, null)
        }
        val job = launch { recorder().run() }
        runCurrent()
        assertEquals(t.plusHours(2).plusMinutes(30), repo.sessions.value[3L]!!.endedAt)
        // keep = 2
        assertEquals(setOf(2L, 3L), repo.sessions.value.keys)
        job.cancel()
    }
}
