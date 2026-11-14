package com.potatoes.book5.realestate.domain.policy

import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.model.Scrap
import kotlin.math.roundToInt

/** 오각형 축. 순서는 모든 오각형에서 같다(SCR-010 오각형 규칙). */
enum class MatchAxis { PRICE, AREA, NEIGHBORHOOD, HOUSEHOLDS, NEWNESS }

/**
 * 단지가 관심 조건을 얼마나 만족하는지.
 * @property scores 축별 0~5. 단지 정보를 몰라 계산할 수 없으면 null
 */
data class ConditionMatch(val scores: Map<MatchAxis, Double?>) {
    /** 계산한 축의 평균 × 20, 반올림 백분율. 계산한 축이 없으면 0 */
    val ratePercent: Int
        get() {
            val known = scores.values.filterNotNull()
            return if (known.isEmpty()) 0 else (known.average() * 20).roundToInt()
        }
}

/**
 * 스크랩 단지와 관심 조건을 비교한다(T-017 계획 "오각형 계산", 제안값).
 * - 조건 범위 안이면 5, 벗어난 정도만큼 줄여 0~5로 자른다.
 * - 조건을 고르지 않은 축은 5로 본다.
 */
object ConditionMatchPolicy {
    const val MAX_SCORE = 5.0

    /** 가격대: 가장 가까운 경계에서 10% 벗어날 때마다 −1 */
    private const val PRICE_RATIO_PER_POINT = 0.10
    /** 평수: 3평마다 −1 */
    private const val PYEONG_PER_POINT = 3.0
    /** 세대수: 100세대마다 −1 */
    private const val HOUSEHOLDS_PER_POINT = 100.0
    /** 신축: 2년마다 −1 */
    private const val YEARS_PER_POINT = 2.0

    fun match(condition: InterestCondition, scrap: Scrap, currentYear: Int): ConditionMatch = ConditionMatch(
        mapOf(
            MatchAxis.PRICE to priceScore(condition.priceRanges, scrap.last.priceManwon),
            MatchAxis.AREA to areaScore(condition.areaRanges, scrap.areaPyeong),
            MatchAxis.NEIGHBORHOOD to neighborhoodScore(condition, scrap),
            MatchAxis.HOUSEHOLDS to householdScore(condition, scrap.households),
            MatchAxis.NEWNESS to newnessScore(condition, scrap.builtYear, currentYear),
        )
    )

    internal fun priceScore(ranges: Set<PriceRange>, priceManwon: Long): Double {
        if (ranges.isEmpty()) return MAX_SCORE
        val penalty = ranges.minOf { range ->
            val (low, high) = range.boundsManwon()
            when {
                priceManwon < low -> (low - priceManwon).toDouble() / low / PRICE_RATIO_PER_POINT
                high != null && priceManwon >= high -> (priceManwon - high).toDouble() / high / PRICE_RATIO_PER_POINT
                else -> 0.0
            }
        }
        return clamp(MAX_SCORE - penalty)
    }

    internal fun areaScore(ranges: Set<AreaRange>, pyeong: Int): Double {
        if (ranges.isEmpty()) return MAX_SCORE
        val penalty = ranges.minOf { range ->
            val (low, high) = range.boundsPyeong()
            when {
                pyeong < low -> (low - pyeong) / PYEONG_PER_POINT
                high != null && pyeong >= high -> (pyeong - high + 1) / PYEONG_PER_POINT
                else -> 0.0
            }
        }
        return clamp(MAX_SCORE - penalty)
    }

    private fun neighborhoodScore(condition: InterestCondition, scrap: Scrap): Double = when {
        condition.neighborhoods.isEmpty() -> MAX_SCORE
        condition.neighborhoods.any {
            if (scrap.apartmentId.startsWith("molit:")) {
                val districtCode = scrap.apartmentId.split(':').getOrNull(1)
                it.code == "molit:$districtCode:dong:${scrap.neighborhood}"
            } else it.name == scrap.neighborhood
        } -> MAX_SCORE
        else -> 0.0
    }

    private fun householdScore(condition: InterestCondition, households: Int?): Double? {
        val min = condition.minHouseholds ?: return MAX_SCORE
        households ?: return null
        val shortage = (min.households - households).coerceAtLeast(0)
        return clamp(MAX_SCORE - shortage / HOUSEHOLDS_PER_POINT)
    }

    private fun newnessScore(condition: InterestCondition, builtYear: Int?, currentYear: Int): Double? {
        val age = condition.maxBuildingAge ?: return MAX_SCORE
        builtYear ?: return null
        val over = (currentYear - builtYear - age.maxYears).coerceAtLeast(0)
        return clamp(MAX_SCORE - over / YEARS_PER_POINT)
    }

    private fun clamp(v: Double) = v.coerceIn(0.0, MAX_SCORE)

    /** 만원 단위 [하한, 상한). 상한이 없으면 null(F-004 구간, 제안값) */
    private fun PriceRange.boundsManwon(): Pair<Long, Long?> = when (this) {
        PriceRange.UNDER_5 -> 0L to 50_000L
        PriceRange.FROM_5_TO_10 -> 50_000L to 100_000L
        PriceRange.FROM_10_TO_15 -> 100_000L to 150_000L
        PriceRange.FROM_15_TO_20 -> 150_000L to 200_000L
        PriceRange.OVER_20 -> 200_000L to null
    }

    /** 평 [하한, 상한). 상한이 없으면 null */
    private fun AreaRange.boundsPyeong(): Pair<Int, Int?> = when (this) {
        AreaRange.UNDER_20 -> 0 to 20
        AreaRange.PY_20S -> 20 to 30
        AreaRange.PY_30S -> 30 to 40
        AreaRange.OVER_40 -> 40 to null
    }
}
