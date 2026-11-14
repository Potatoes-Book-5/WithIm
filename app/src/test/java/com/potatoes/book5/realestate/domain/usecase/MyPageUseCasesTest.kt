package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.PriceChange
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.model.ScrapChangeKind
import com.potatoes.book5.realestate.fakes.FakeApartmentRepository
import com.potatoes.book5.realestate.fakes.FakeInterestRepository
import com.potatoes.book5.realestate.fakes.FakeScrapRepository
import com.potatoes.book5.realestate.fakes.dealOf
import com.potatoes.book5.realestate.fakes.scrapOf
import java.time.LocalDateTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyPageUseCasesTest {

    private val t0 = LocalDateTime.of(2026, 11, 1, 9, 0)

    @Test
    fun `일치율 높은 순으로 3곳을 고르고 같으면 최근 스크랩이 먼저다`() = runTest {
        val interest = FakeInterestRepository(InterestCondition(priceRanges = setOf(PriceRange.FROM_10_TO_15)))
        val scraps = FakeScrapRepository(
            listOf(
                scrapOf("far", price = 300_000, scrappedAt = t0),
                scrapOf("in-old", price = 120_000, scrappedAt = t0),
                scrapOf("in-new", price = 130_000, scrappedAt = t0.plusDays(1)),
                scrapOf("near", price = 160_000, scrappedAt = t0),
                scrapOf("changed", price = 400_000, scrappedAt = t0, change = PriceChange(ScrapChangeKind.NEW_DEAL, 1, t0, seen = false)),
            )
        )
        val data = ObserveMyPageUseCase(interest, scraps) { 2026 }().first()
        assertEquals(listOf("in-new", "in-old", "near"), data.topMatches.map { it.scrap.apartmentId })
        assertEquals(5, data.scrapCount)
        assertEquals(1, data.changedCount)
    }

    @Test
    fun `조건이 없으면 비교하지 않고, 조건을 바꾸면 다시 계산한다`() = runTest {
        val interest = FakeInterestRepository()
        val scraps = FakeScrapRepository(listOf(scrapOf("a")))
        val useCase = ObserveMyPageUseCase(interest, scraps) { 2026 }
        val empty = useCase().first()
        assertNull(empty.condition)
        assertTrue(empty.topMatches.isEmpty())
        assertEquals(1, empty.scrapCount)

        interest.save(InterestCondition(areaRanges = setOf(AreaRange.PY_30S)))
        assertEquals(1, useCase().first().topMatches.size)
    }

    @Test
    fun `변동 확인 때 세대수와 준공연도가 없는 스크랩을 채운다`() = runTest {
        val scraps = FakeScrapRepository(listOf(scrapOf("a", price = 150_000)))
        val apartments = FakeApartmentRepository(mutableMapOf("a" to dealOf(150_000)))
        RefreshScrapChangesUseCase(scraps, apartments) { t0 }()
        val filled = scraps.get("a")!!
        assertEquals(1_000, filled.households)
        assertEquals(2010, filled.builtYear)
    }

    @Test
    fun `스크랩할 때 세대수와 준공연도를 함께 저장한다`() = runTest {
        val scraps = FakeScrapRepository()
        ToggleScrapUseCase(scraps) { t0 }(com.potatoes.book5.realestate.fakes.detailOf("a", dealOf(150_000)))
        assertEquals(1_000, scraps.get("a")!!.households)
        assertEquals(2010, scraps.get("a")!!.builtYear)
    }
}
