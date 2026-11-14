package com.potatoes.book5.realestate.data.sample

import com.potatoes.book5.realestate.domain.policy.GeoMath
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SampleRankingRepositoryTest {

    @Test
    fun `샘플 단지 좌표는 차량 기준 거리와 맞는다`() = runTest {
        val repository = SampleRankingRepository()
        repeat(2) {
            val snapshot = repository.observeNearbyRanking().first()
            val vehicle = snapshot.vehicle
            assertNotNull(vehicle)
            vehicle!!
            (snapshot.top + snapshot.interestMatches + snapshot.others).forEach { nearby ->
                val location = nearby.apartment.location!!
                assertEquals(nearby.distanceKm, GeoMath.distanceKm(vehicle.location, location), 1e-6)
            }
            repository.moveToNextArea()
        }
    }
}
