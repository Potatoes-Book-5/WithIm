package com.potatoes.book5.realestate.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.potatoes.book5.realestate.data.local.db.AppDatabase
import com.potatoes.book5.realestate.domain.model.DriveLeaderChange
import com.potatoes.book5.realestate.domain.model.DriveRank
import com.potatoes.book5.realestate.domain.model.PassedInterest
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** L2: 주행 기록 저장 계약(F-009, T-018). */
@RunWith(AndroidJUnit4::class)
class RoomDriveRecordRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RoomDriveRecordRepository
    private val t = LocalDateTime.of(2026, 11, 14, 10, 0)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = RoomDriveRecordRepository(db, ZoneId.of("Asia/Seoul"))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun recordFinishAndObserveSummary() = runTest {
        val id = repo.startSession(t, 10)
        assertEquals(id, repo.openSessionId())
        repo.recordSnapshot(id, t.plusMinutes(5), listOf(DriveRank(1, "a", "A", 100), DriveRank(2, "b", "B", 90)), listOf(PassedInterest("h", "H", 3.0, 150)))
        repo.recordLeaderChange(id, DriveLeaderChange(t.plusMinutes(10), "b", "B"))
        repo.recordSnapshot(id, t.plusMinutes(10), listOf(DriveRank(1, "b", "B", 90)), listOf(PassedInterest("h", "H", 1.2, 150), PassedInterest("m", "M", 2.0, 130)))
        // 닫기 전에는 요약이 없다.
        assertNull(repo.observeSummary(id).first())

        assertTrue(repo.finishSession(id, t.plusMinutes(12)))
        assertNull(repo.openSessionId())
        val summary = repo.observeSummary(id).first()!!
        assertEquals(t.plusMinutes(12), summary.endedAt)
        assertEquals(listOf("b"), summary.finalTop.map { it.apartmentId })
        assertEquals(listOf("b"), summary.leaderChanges.map { it.apartmentId })
        assertEquals(listOf("h" to 1.2, "m" to 2.0), summary.passedInterests.map { it.apartmentId to it.closestKm })
        assertEquals(id, repo.observeLatestUnseenId().first())

        repo.markSeen(id)
        assertNull(repo.observeLatestUnseenId().first())
        assertEquals(id, repo.observeLatestId().first())
    }

    @Test
    fun deleteAllRemovesSessionsAndChildRows() = runTest {
        val id = repo.startSession(t, 10)
        repo.recordSnapshot(id, t, listOf(DriveRank(1, "a", "A", 1)), listOf(PassedInterest("h", "H", 1.0, 1)))
        repo.recordLeaderChange(id, DriveLeaderChange(t, "a", "A"))
        repo.finishSession(id, null)
        repo.deleteAll()
        assertNull(repo.observeLatestId().first())
        listOf("drive_ranks", "drive_leader_changes", "drive_passed_interests").forEach { table ->
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
        }
    }

    @Test
    fun alertedRecommendationIsKeptAsPassedInterestEvenOutsideFinalTopThree() = runTest {
        repo.recordAlertedInterest(t.plusMinutes(2), 10, PassedInterest("alerted", "알림 단지", 1.8, 150))
        val id = repo.openSessionId()!!
        // The recorder reuses the session if the notification created it first.
        assertEquals(id, repo.startSession(t.plusMinutes(2), 10))
        repo.recordAlertedInterest(t.plusMinutes(5), 10, PassedInterest("alerted", "알림 단지", 0.7, 150))
        repo.recordSnapshot(id, t.plusMinutes(6), emptyList(), emptyList())

        assertTrue(repo.finishSession(id, t.plusMinutes(7)))
        val summary = repo.observeSummary(id).first()!!
        assertEquals(listOf("alerted" to 0.7), summary.passedInterests.map { it.apartmentId to it.closestKm })
    }

    @Test
    fun emptySessionIsDeletedAndTrimKeepsRecent() = runTest {
        val empty = repo.startSession(t, 10)
        assertFalse(repo.finishSession(empty, null))
        assertNull(repo.observeLatestId().first())

        val ids = (0 until 3).map { i ->
            val id = repo.startSession(t.plusHours(i.toLong()), 10)
            repo.recordSnapshot(id, t.plusHours(i.toLong()).plusMinutes(30), listOf(DriveRank(1, "a", "A", 1)), emptyList())
            repo.recordLeaderChange(id, DriveLeaderChange(t, "a", "A"))
            repo.finishSession(id, null)
            id
        }
        repo.trim(2)
        assertNull(repo.observeSummary(ids[0]).first())
        assertEquals(ids[2], repo.observeLatestId().first())
        // 마지막 기록 시각으로 닫혔다.
        assertEquals(t.plusHours(2).plusMinutes(30), repo.observeSummary(ids[2]).first()!!.endedAt)
    }
}
