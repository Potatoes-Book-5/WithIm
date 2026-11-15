package com.potatoes.book5.realestate.domain

import com.potatoes.book5.realestate.domain.model.Apartment
import com.potatoes.book5.realestate.domain.model.DriveRecommendation
import com.potatoes.book5.realestate.domain.model.GeoPoint
import com.potatoes.book5.realestate.domain.model.InterestCondition
import com.potatoes.book5.realestate.domain.model.NearbyApartment
import com.potatoes.book5.realestate.domain.model.Neighborhood
import com.potatoes.book5.realestate.domain.model.PriceRecord
import com.potatoes.book5.realestate.domain.model.RecommendationFeedback
import com.potatoes.book5.realestate.domain.model.Scrap
import com.potatoes.book5.realestate.domain.policy.DriveRecommendationPolicy
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DriveRecommendationPersonalizationTest {
    private val condition = InterestCondition(neighborhoods = setOf(Neighborhood("a", "A")))
    private val candidates = listOf(
        nearby("explicit", "A", "a", distance = 1.0),
        nearby("behavior", "B", "b", distance = 2.0),
    )

    @Test
    fun `no user behavior preserves explicit-condition ranking`() {
        val result = DriveRecommendationPolicy.rank(condition, candidates, 2026)

        assertEquals(listOf("explicit", "behavior"), result.map { it.apartmentId })
    }

    @Test
    fun `repeated recommendation detail opens increase matching neighborhood preference`() {
        val feedback = listOf("sample-1", "sample-2").map { id ->
            RecommendationFeedback(id, 100_000, 32, "B", 1_000, 2018, detailOpenCount = 10)
        }

        val result = DriveRecommendationPolicy.rank(condition, candidates, 2026, feedback = feedback)

        assertEquals(listOf("behavior", "explicit"), result.map { it.apartmentId })
    }

    @Test
    fun `active scraps contribute profile examples and duplicate detail history is not double counted`() {
        val saved = (1..5).map { scrap(if (it == 1) "behavior" else "saved-$it") }
        val feedback = listOf(RecommendationFeedback("behavior", 100_000, 32, "B", 1_000, 2018, 10))
        val withBoth = DriveRecommendationPolicy.rank(condition, candidates, 2026, scraps = saved, feedback = feedback)
        val withScrap = DriveRecommendationPolicy.rank(condition, candidates, 2026, scraps = saved)

        assertEquals(withScrap, withBoth)
        assertEquals("behavior", withBoth.first().apartmentId)
    }

    private fun nearby(id: String, neighborhood: String, code: String, distance: Double) = NearbyApartment(
        apartment = Apartment(id, id, 100_000, 32, LocalDate.of(2026, 10, 1), GeoPoint(0.0, 0.0), code, neighborhood, 1_000, 2018),
        distanceKm = distance,
        bearingDeg = 0.0,
    )

    private fun scrap(id: String): Scrap {
        val at = LocalDateTime.of(2026, 1, 1, 0, 0)
        val price = PriceRecord(100_000, LocalDate.of(2025, 12, 1))
        return Scrap(id, id, "B", 32, at, price, price, at, null, households = 1_000, builtYear = 2018)
    }
}
