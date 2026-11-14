package com.potatoes.book5.realestate.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.potatoes.book5.realestate.data.local.db.AppDatabase
import com.potatoes.book5.realestate.domain.model.PriceChange
import com.potatoes.book5.realestate.domain.model.PriceRecord
import com.potatoes.book5.realestate.domain.model.Scrap
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import java.time.LocalDate
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

/** L2: 실제 SQLite(메모리 DB)에서 스크랩 저장 계약을 확인한다(ADR-005). */
@RunWith(AndroidJUnit4::class)
class RoomScrapRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RoomScrapRepository
    private val zone = ZoneId.of("Asia/Seoul")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java).build()
        repo = RoomScrapRepository(db.scrapDao(), zone)
    }

    @After
    fun tearDown() = db.close()

    private fun scrap(id: String, scrappedAt: LocalDateTime, change: PriceChange? = null): Scrap {
        val record = PriceRecord(150_000, LocalDate.of(2026, 10, 1))
        return Scrap(id, "단지-$id", "정자동", 34, scrappedAt, record, record, scrappedAt, change)
    }

    @Test
    fun saveReadDeleteAndObserve() = runTest {
        val older = scrap("a", LocalDateTime.of(2026, 10, 1, 9, 0))
        val newer = scrap("b", LocalDateTime.of(2026, 11, 1, 9, 0))
        repo.upsert(older)
        repo.upsert(newer)

        assertEquals(listOf(newer, older), repo.observeAll().first())
        assertEquals(older, repo.get("a"))
        assertTrue(repo.observeIsScrapped("a").first())

        repo.delete("a")
        assertNull(repo.get("a"))
        assertFalse(repo.observeIsScrapped("a").first())
        assertEquals(listOf(newer), repo.getAll())
    }

    @Test
    fun upsertReplacesAndMarkSeenOnlyTouchesChanges() = runTest {
        val at = LocalDateTime.of(2026, 11, 14, 10, 0)
        repo.upsert(scrap("a", at))
        val changed = scrap("a", at).copy(
            last = PriceRecord(153_000, LocalDate.of(2026, 10, 8)),
            change = PriceChange(ScrapChangeKind.NEW_DEAL, 3_000, at, seen = false),
        )
        repo.upsert(changed)
        assertEquals(1, repo.getAll().size)
        assertEquals(changed, repo.get("a"))

        repo.markChangeSeen("a")
        assertEquals(true, repo.get("a")!!.change?.seen)

        // 변동이 없는 스크랩은 확인 처리해도 그대로다.
        repo.upsert(scrap("b", at))
        repo.markChangeSeen("b")
        assertNull(repo.get("b")!!.change)
    }
}
