package com.potatoes.book5.realestate.domain.policy

import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.BuildingAge
import com.potatoes.book5.realestate.domain.model.HouseholdMin
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.fakes.scrapOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConditionMatchPolicyTest {

    private val year = 2026
    private val full = InterestCondition(
        neighborhoods = setOf(Neighborhood("c", "정자동")),
        priceRanges = setOf(PriceRange.FROM_10_TO_15),
        areaRanges = setOf(AreaRange.PY_30S),
        minHouseholds = HouseholdMin.OVER_500,
        maxBuildingAge = BuildingAge.WITHIN_10,
    )

    @Test
    fun `모든 조건 안이면 다섯 축 모두 5이고 일치율 100`() {
        val scrap = scrapOf("a", price = 140_000).copy(households = 800, builtYear = 2020)
        val match = ConditionMatchPolicy.match(full, scrap, year)
        assertEquals(List(5) { 5.0 }, MatchAxis.entries.map { match.scores[it] })
        assertEquals(100, match.ratePercent)
    }

    @Test
    fun `가격은 가까운 구간 경계에서 10퍼센트마다 1점 깎는다`() {
        val ranges = setOf(PriceRange.FROM_10_TO_15)
        assertEquals(5.0, ConditionMatchPolicy.priceScore(ranges, 150_000), 1e-9)
        // 15억보다 1.5억(10%) 높으면 4점
        assertEquals(4.0, ConditionMatchPolicy.priceScore(ranges, 165_000), 1e-9)
        // 10억보다 2억(20%) 낮으면 3점
        assertEquals(3.0, ConditionMatchPolicy.priceScore(ranges, 80_000), 1e-9)
        assertEquals(0.0, ConditionMatchPolicy.priceScore(ranges, 300_000), 1e-9)
        // 여러 구간이면 가장 가까운 구간 기준
        assertEquals(5.0, ConditionMatchPolicy.priceScore(ranges + PriceRange.OVER_20, 250_000), 1e-9)
        assertEquals(5.0, ConditionMatchPolicy.priceScore(emptySet(), 1_000_000), 1e-9)
    }

    @Test
    fun `평수는 3평마다 1점 깎는다`() {
        val ranges = setOf(AreaRange.PY_30S)
        assertEquals(5.0, ConditionMatchPolicy.areaScore(ranges, 39), 1e-9)
        assertEquals(4.0, ConditionMatchPolicy.areaScore(ranges, 27), 1e-9)
        assertEquals(4.0, ConditionMatchPolicy.areaScore(ranges, 42), 1e-9)
    }

    @Test
    fun `동네 세대수 신축 축을 계산하고 모르는 값은 평균에서 뺀다`() {
        val scrap = scrapOf("a", price = 140_000).copy(neighborhood = "서현동", households = 300, builtYear = 2010)
        val match = ConditionMatchPolicy.match(full, scrap, year)
        assertEquals(0.0, match.scores[MatchAxis.NEIGHBORHOOD]!!, 1e-9)
        // 500세대보다 200세대 적음 → 3
        assertEquals(3.0, match.scores[MatchAxis.HOUSEHOLDS]!!, 1e-9)
        // 16년차, 10년보다 6년 오래됨 → 2
        assertEquals(2.0, match.scores[MatchAxis.NEWNESS]!!, 1e-9)
        // (5 + 5 + 0 + 3 + 2) / 5 × 20 = 60
        assertEquals(60, match.ratePercent)

        val unknown = ConditionMatchPolicy.match(full, scrapOf("b", price = 140_000), year)
        assertNull(unknown.scores[MatchAxis.HOUSEHOLDS])
        assertNull(unknown.scores[MatchAxis.NEWNESS])
        assertEquals(100, unknown.ratePercent)
    }

    @Test
    fun `고르지 않은 조건은 만족한 것으로 본다`() {
        val match = ConditionMatchPolicy.match(InterestCondition(priceRanges = setOf(PriceRange.UNDER_5)), scrapOf("a", price = 50_000), year)
        assertEquals(5.0, match.scores[MatchAxis.AREA]!!, 1e-9)
        assertEquals(5.0, match.scores[MatchAxis.HOUSEHOLDS]!!, 1e-9)
        assertEquals(5.0, match.scores[MatchAxis.PRICE]!!, 1e-9)
    }
}
