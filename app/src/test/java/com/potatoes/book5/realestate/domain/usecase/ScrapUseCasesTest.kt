package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.model.PriceChange
import com.potatoes.book5.realestate.domain.model.PriceRecord
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.domain.policy.DetectedChange
import com.potatoes.book5.realestate.domain.policy.ScrapChangePolicy
import com.potatoes.book5.realestate.fakes.FakeApartmentRepository
import com.potatoes.book5.realestate.fakes.FakeScrapRepository
import com.potatoes.book5.realestate.fakes.dealOf
import com.potatoes.book5.realestate.fakes.detailOf
import com.potatoes.book5.realestate.fakes.scrapOf
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScrapUseCasesTest {

    private val oct1 = LocalDate.of(2026, 10, 1)
    private val now = LocalDateTime.of(2026, 11, 14, 10, 0)

    @Test
    fun `변동 판단 - 거래일이 늦으면 새 실거래, 같은 날 가격만 다르면 상승 하락`() {
        val last = PriceRecord(150_000, oct1)
        assertEquals(DetectedChange(ScrapChangeKind.NEW_DEAL, 3_000), ScrapChangePolicy.detect(last, dealOf(153_000, oct1.plusDays(5))))
        // 새 거래가 더 싸도 새 실거래로 본다.
        assertEquals(DetectedChange(ScrapChangeKind.NEW_DEAL, -1_000), ScrapChangePolicy.detect(last, dealOf(149_000, oct1.plusDays(5))))
        assertEquals(DetectedChange(ScrapChangeKind.PRICE_UP, 500), ScrapChangePolicy.detect(last, dealOf(150_500, oct1)))
        assertEquals(DetectedChange(ScrapChangeKind.PRICE_DOWN, -500), ScrapChangePolicy.detect(last, dealOf(149_500, oct1)))
        assertNull(ScrapChangePolicy.detect(last, dealOf(150_000, oct1)))
        // 더 오래된 거래가 최신으로 들어와도 변동으로 보지 않는다.
        assertNull(ScrapChangePolicy.detect(last, dealOf(160_000, oct1.minusDays(3))))
    }

    @Test
    fun `스크랩하면 최근 실거래를 기준 값으로 저장하고 다시 누르면 해제한다`() = runTest {
        val repo = FakeScrapRepository()
        val toggle = ToggleScrapUseCase(repo) { now }
        val detail = detailOf("a", dealOf(158_000, oct1, pyeong = 34))

        assertTrue(toggle(detail))
        val saved = repo.get("a")!!
        assertEquals(PriceRecord(158_000, oct1), saved.baseline)
        assertEquals(saved.baseline, saved.last)
        assertEquals(34, saved.areaPyeong)
        assertEquals(now, saved.scrappedAt)
        assertNull(saved.change)

        assertFalse(toggle(detail))
        assertNull(repo.get("a"))
    }

    @Test
    fun `변동 확인은 새 거래를 기록하고 실패한 단지는 그대로 둔다`() = runTest {
        val repo = FakeScrapRepository(listOf(scrapOf("a", 150_000, oct1), scrapOf("b", 120_000, oct1), scrapOf("c", 100_000, oct1)))
        val apartments = FakeApartmentRepository(
            deals = mutableMapOf("a" to dealOf(153_000, oct1.plusDays(7)), "b" to dealOf(120_000, oct1)),
            failing = setOf("c"),
        )
        val refresh = RefreshScrapChangesUseCase(repo, apartments) { now }

        assertEquals(1, refresh())
        val a = repo.get("a")!!
        assertEquals(PriceChange(ScrapChangeKind.NEW_DEAL, 3_000, now, seen = false), a.change)
        assertEquals(PriceRecord(153_000, oct1.plusDays(7)), a.last)
        assertEquals(PriceRecord(150_000, oct1), a.baseline)
        assertNull(repo.get("b")!!.change)
        assertEquals(now, repo.get("b")!!.lastCheckedAt)
        assertEquals(scrapOf("c", 100_000, oct1), repo.get("c"))

        // 같은 거래로 다시 확인하면 새 변동이 없고, 기존 변동은 남는다.
        assertEquals(0, refresh())
        assertEquals(ScrapChangeKind.NEW_DEAL, repo.get("a")!!.change?.kind)
    }

    @Test
    fun `요약은 확인하지 않은 변동만 세고 목록은 변동 단지를 먼저 둔다`() = runTest {
        val seen = PriceChange(ScrapChangeKind.PRICE_UP, 1_000, now.minusDays(2), seen = true)
        val unseenOld = PriceChange(ScrapChangeKind.PRICE_DOWN, -1_000, now.minusDays(1), seen = false)
        val unseenNew = PriceChange(ScrapChangeKind.NEW_DEAL, 2_000, now, seen = false)
        val repo = FakeScrapRepository(
            listOf(
                scrapOf("recent", scrappedAt = now),
                scrapOf("seen", scrappedAt = now.minusDays(1), change = seen),
                scrapOf("old-change", scrappedAt = now.minusDays(9), change = unseenOld),
                scrapOf("new-change", scrappedAt = now.minusDays(10), change = unseenNew),
            )
        )
        val summary = ObserveScrapSummaryUseCase(repo)().first()
        assertEquals(4, summary.totalCount)
        assertEquals(2, summary.changedCount)
        assertEquals("단지-new-change", summary.firstChange?.apartmentName)

        val order = ObserveScrapsUseCase(repo)().first().map { it.apartmentId }
        assertEquals(listOf("old-change", "new-change", "recent", "seen"), order)

        MarkScrapChangeSeenUseCase(repo)("new-change")
        assertEquals(1, ObserveScrapSummaryUseCase(repo)().first().changedCount)
    }
}
