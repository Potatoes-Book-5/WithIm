package com.potatoes.book5.realestate.domain.policy

import com.potatoes.book5.realestate.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {
    private val origin = GeoPoint(37.3670, 127.1080)

    @Test
    fun `북쪽으로 가면 위도만 늘어난다`() {
        val p = GeoMath.destination(origin, 1.0, 0.0)
        assertEquals(origin.latitude + 0.008993, p.latitude, 1e-5)
        assertEquals(origin.longitude, p.longitude, 1e-9)
    }

    @Test
    fun `동쪽으로 가면 경도가 늘어난다`() {
        val p = GeoMath.destination(origin, 2.0, 90.0)
        assertTrue(p.longitude > origin.longitude)
        assertEquals(origin.latitude, p.latitude, 1e-4)
    }

    @Test
    fun `계산한 지점까지의 거리는 입력 거리와 같다`() {
        listOf(0.0, 45.0, 135.0, 200.0, 315.0).forEach { bearing ->
            val p = GeoMath.destination(origin, 6.8, bearing)
            assertEquals(6.8, GeoMath.distanceKm(origin, p), 1e-6)
        }
    }
}
