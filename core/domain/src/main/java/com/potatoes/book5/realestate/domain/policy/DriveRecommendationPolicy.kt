package com.potatoes.book5.realestate.domain.policy

import com.potatoes.book5.realestate.domain.model.AreaRange
import com.potatoes.book5.realestate.domain.model.Apartment
import com.potatoes.book5.realestate.domain.model.DriveRecommendation
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.model.PriceRange
import com.potatoes.book5.realestate.domain.model.RecommendationFeedback
import com.potatoes.book5.realestate.domain.model.RecommendationKey
import com.potatoes.book5.realestate.domain.model.Scrap
import kotlin.math.roundToInt

/** Deterministic preference fit ranking for one drive session (F-010). */
object DriveRecommendationPolicy {
    private const val MAX = 5.0

    fun rank(
        condition: InterestCondition,
        candidates: List<NearbyApartment>,
        currentYear: Int,
        scraps: List<Scrap> = emptyList(),
        feedback: List<RecommendationFeedback> = emptyList(),
    ): List<DriveRecommendation> {
        if (condition.isEmpty) return emptyList()
        val profile = PreferenceProfile.from(scraps, feedback)
        return candidates.mapNotNull { candidate -> evaluate(condition, candidate, currentYear, profile) }
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
        profile: PreferenceProfile,
    ): DriveRecommendation? {
        val apartment = candidate.apartment
        val explicitScores = buildMap {
            if (condition.priceRanges.isNotEmpty()) put(RecommendationKey.PRICE, priceScore(condition.priceRanges, apartment.priceManwon))
            if (condition.areaRanges.isNotEmpty()) put(RecommendationKey.AREA, areaScore(condition.areaRanges, apartment.areaPyeong))
            if (condition.neighborhoods.isNotEmpty()) {
                val code = apartment.neighborhoodCode
                if (code != null) put(RecommendationKey.NEIGHBORHOOD,
                    if (condition.neighborhoods.any { it.code == code }) MAX else 0.0)
            }
            condition.minHouseholds?.let { min ->
                apartment.households?.let { count ->
                    put(RecommendationKey.HOUSEHOLDS, (MAX - (min.households - count).coerceAtLeast(0) / 100.0).coerceIn(0.0, MAX))
                }
            }
            condition.maxBuildingAge?.let { limit ->
                apartment.builtYear?.let { built ->
                    val overBy = (currentYear - built - limit.maxYears).coerceAtLeast(0)
                    put(RecommendationKey.NEWNESS, (MAX - overBy / 2.0).coerceIn(0.0, MAX))
                }
            }
        }
        val implicitScores = profile.similarity(apartment)
        if (explicitScores.isEmpty() && implicitScores.isEmpty()) return null
        val behaviorWeight = profile.behaviorWeight
        val explicitWeight = 1.0 - behaviorWeight
        val keys = RecommendationKey.entries.filter { it in explicitScores || it in implicitScores }
        val blended = keys.associateWith { key ->
            val explicit = explicitScores[key]
            val implicit = implicitScores[key]
            when {
                explicit != null && implicit != null -> explicit * explicitWeight + implicit * behaviorWeight
                explicit != null -> explicit
                else -> requireNotNull(implicit)
            }
        }
        // Enum order is the deterministic priority for ties, matching the legacy score policy.
        val mainKey = blended.maxBy { it.value }.key
        val weights = keys.sumOf { key ->
            (if (key in explicitScores) explicitWeight else 0.0) +
                (if (key in implicitScores) behaviorWeight else 0.0)
        }
        val weightedScore = keys.sumOf { key -> blended.getValue(key) } / keys.size
        // Keep legacy equal-axis means when no behavior exists; with behavior, each available axis contribution
        // is weighted by explicit/profile confidence rather than giving a sparse implicit axis full influence.
        val score = if (behaviorWeight == 0.0) weightedScore else {
            val numerator = keys.sumOf { key ->
                (explicitScores[key] ?: 0.0) * explicitWeight + (implicitScores[key] ?: 0.0) * behaviorWeight
            }
            numerator / weights.coerceAtLeast(1e-9)
        }
        val scorePercent = (score * 20.0).roundToInt()
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

    private data class PreferenceExample(
        val price: Long?,
        val area: Int?,
        val neighborhood: String?,
        val households: Int?,
        val builtYear: Int?,
        val weight: Double,
    )

    private class PreferenceProfile private constructor(
        private val effectiveExamples: Double,
        private val averagePrice: Double?,
        private val averageArea: Double?,
        private val neighborhoodWeights: Map<String, Double>,
        private val neighborhoodWeight: Double,
        private val averageHouseholds: Double?,
        private val averageBuiltYear: Double?,
    ) {
        val behaviorWeight: Double = (effectiveExamples * 0.14).coerceAtMost(0.7)

        fun similarity(apartment: Apartment): Map<RecommendationKey, Double> = buildMap {
            similarity(apartment.priceManwon.toDouble(), averagePrice, 50_000.0)
                ?.let { put(RecommendationKey.PRICE, it) }
            similarity(apartment.areaPyeong.toDouble(), averageArea, 15.0)
                ?.let { put(RecommendationKey.AREA, it) }
            val candidateNeighborhood = apartment.neighborhoodName?.trim()?.lowercase()?.takeIf(String::isNotEmpty)
            if (candidateNeighborhood != null && neighborhoodWeight > 0.0)
                put(RecommendationKey.NEIGHBORHOOD, MAX * (neighborhoodWeights[candidateNeighborhood] ?: 0.0) / neighborhoodWeight)
            apartment.households?.let { value ->
                similarity(value.toDouble(), averageHouseholds, 1_500.0)
                    ?.let { put(RecommendationKey.HOUSEHOLDS, it) }
            }
            apartment.builtYear?.let { value ->
                similarity(value.toDouble(), averageBuiltYear, 25.0)
                    ?.let { put(RecommendationKey.NEWNESS, it) }
            }
        }

        private fun similarity(candidate: Double, preference: Double?, scale: Double): Double? = preference?.let {
            MAX * (1.0 - kotlin.math.abs(candidate - it) / scale).coerceIn(0.0, 1.0)
        }

        companion object {
            fun from(scraps: List<Scrap>, feedback: List<RecommendationFeedback>): PreferenceProfile {
                val scrapsById = scraps.associateBy { it.apartmentId }
                val feedbackById = feedback.associateBy { it.apartmentId }
                val ids = scrapsById.keys + feedbackById.keys
                val examples = ids.mapNotNull { id ->
                    val scrap = scrapsById[id]
                    val response = feedbackById[id]
                    val weight = if (scrap != null) 1.0 else {
                        (response?.detailOpenCount ?: 0).coerceAtMost(10) * 0.25
                    }
                    if (weight <= 0.0) return@mapNotNull null
                    PreferenceExample(
                        price = scrap?.last?.priceManwon ?: response?.priceManwon,
                        area = scrap?.areaPyeong ?: response?.areaPyeong,
                        neighborhood = scrap?.neighborhood ?: response?.neighborhoodName,
                        households = scrap?.households ?: response?.households,
                        builtYear = scrap?.builtYear ?: response?.builtYear,
                        weight = weight,
                    )
                }
                fun average(selector: (PreferenceExample) -> Double?): Double? {
                    val values = examples.mapNotNull { example -> selector(example)?.let { it to example.weight } }
                    if (values.isEmpty()) return null
                    val totalWeight = values.sumOf { it.second }
                    return values.sumOf { (value, weight) -> value * weight } / totalWeight
                }
                val neighborhoodExamples = examples.mapNotNull { example ->
                    example.neighborhood?.trim()?.lowercase()?.takeIf(String::isNotEmpty)?.let { it to example.weight }
                }
                return PreferenceProfile(
                    effectiveExamples = examples.sumOf { it.weight },
                    averagePrice = average { it.price?.toDouble() },
                    averageArea = average { it.area?.toDouble() },
                    neighborhoodWeights = neighborhoodExamples.groupBy({ it.first }, { it.second })
                        .mapValues { (_, weights) -> weights.sum() },
                    neighborhoodWeight = neighborhoodExamples.sumOf { it.second },
                    averageHouseholds = average { it.households?.toDouble() },
                    averageBuiltYear = average { it.builtYear?.toDouble() },
                )
            }
        }
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
