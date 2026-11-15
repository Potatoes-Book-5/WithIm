package com.potatoes.book5.realestate.domain.policy

import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.DriveRecommendation
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.model.RecommendationKey
import kotlin.math.roundToInt

/** Deterministic preference fit ranking for one drive session (F-010). */
object DriveRecommendationPolicy {
    private const val MAX = 5.0

    fun rank(
        condition: InterestCondition,
        candidates: List<NearbyApartment>,
        currentYear: Int,
    ): List<DriveRecommendation> {
        if (condition.isEmpty) return emptyList()
        return candidates.mapNotNull { candidate -> evaluate(condition, candidate, currentYear) }
            .distinctBy { it.apartmentId }
            .sortedWith(
                compareByDescending<DriveRecommendation> { it.scorePercent }
                    .thenBy { it.closestKm }
                    .thenBy { it.apartmentId },
            )
    }

    private fun evaluate(
        condition: InterestCondition,
        candidate: NearbyApartment,
        currentYear: Int,
    ): DriveRecommendation? {
        val apartment = candidate.apartment
        val scores = buildList {
            if (condition.priceRanges.isNotEmpty()) add(RecommendationKey.PRICE to priceScore(condition.priceRanges, apartment.priceManwon))
            if (condition.areaRanges.isNotEmpty()) add(RecommendationKey.AREA to areaScore(condition.areaRanges, apartment.areaPyeong))
            if (condition.neighborhoods.isNotEmpty()) {
                val code = apartment.neighborhoodCode
                if (code != null) add(RecommendationKey.NEIGHBORHOOD to
                    if (condition.neighborhoods.any { it.code == code }) MAX else 0.0)
            }
            condition.minHouseholds?.let { min ->
                apartment.households?.let { count ->
                    add(RecommendationKey.HOUSEHOLDS to (MAX - (min.households - count).coerceAtLeast(0) / 100.0).coerceIn(0.0, MAX))
                }
            }
            condition.maxBuildingAge?.let { limit ->
                apartment.builtYear?.let { built ->
                    val overBy = (currentYear - built - limit.maxYears).coerceAtLeast(0)
                    add(RecommendationKey.NEWNESS to (MAX - overBy / 2.0).coerceIn(0.0, MAX))
                }
            }
        }
        if (scores.isEmpty()) return null
        // buildList order is the stable priority for equal main-axis scores.
        val mainKey = scores.maxBy { it.second }.first
        val scorePercent = (scores.map { it.second }.average() * 20.0).roundToInt()
        return DriveRecommendation(
            apartmentId = apartment.id,
            name = apartment.name,
            priceManwon = apartment.priceManwon,
            areaPyeong = apartment.areaPyeong,
            scorePercent = scorePercent,
            closestKm = candidate.distanceKm,
            mainRecoKey = mainKey,
            neighborhoodName = apartment.neighborhoodName,
            households = apartment.households,
            builtYear = apartment.builtYear,
        )
    }

    private fun priceScore(ranges: Set<PriceRange>, price: Long): Double {
        val penalty = ranges.minOf { range ->
            val (low, high) = when (range) {
                PriceRange.UNDER_5 -> 0L to 50_000L
                PriceRange.FROM_5_TO_10 -> 50_000L to 100_000L
                PriceRange.FROM_10_TO_15 -> 100_000L to 150_000L
                PriceRange.FROM_15_TO_20 -> 150_000L to 200_000L
                PriceRange.OVER_20 -> 200_000L to null
            }
            when {
                price < low -> (low - price).toDouble() / low / 0.10
                high != null && price >= high -> (price - high).toDouble() / high / 0.10
                else -> 0.0
            }
        }
        return (MAX - penalty).coerceIn(0.0, MAX)
    }

    private fun areaScore(ranges: Set<AreaRange>, area: Int): Double {
        val penalty = ranges.minOf { range ->
            val (low, high) = when (range) {
                AreaRange.UNDER_20 -> 0 to 20
                AreaRange.PY_20S -> 20 to 30
                AreaRange.PY_30S -> 30 to 40
                AreaRange.OVER_40 -> 40 to null
            }
            when {
                area < low -> (low - area) / 3.0
                high != null && area >= high -> (area - high + 1) / 3.0
                else -> 0.0
            }
        }
        return (MAX - penalty).coerceIn(0.0, MAX)
    }
}
