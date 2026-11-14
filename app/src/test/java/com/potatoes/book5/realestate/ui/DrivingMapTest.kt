package com.potatoes.book5.realestate.ui

import com.potatoes.book5.realestate.domain.model.GeoPoint
import com.potatoes.book5.realestate.domain.model.VehiclePose
import com.potatoes.book5.realestate.ui.driving.DrivingResult
import com.potatoes.book5.realestate.ui.driving.DrivingScreenState
import com.potatoes.book5.realestate.ui.driving.reduceDriving
import com.potatoes.book5.realestate.ui.driving.zoomFor
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivingMapTest {

    @Test
    fun `순위 갱신은 차량 위치를 함께 반영한다`() {
        val pose = VehiclePose(GeoPoint(37.0, 127.0), 20.0)
        val state = reduceDriving(DrivingScreenState(), DrivingResult.RankingUpdated(10, emptyList(), emptyList(), emptyList(), pose))
        assertEquals(pose, state.vehicle)
    }

    @Test
    fun `줌은 보이는 길이에 5_5km가 들어가게 정한다`() {
        val metersPerDpAtZoom0 = 60_000.0
        val zoom = zoomFor(metersPerDpAtZoom0, visibleDp = 300f)
        assertEquals(5_500.0, metersPerDpAtZoom0 / 2.0.pow(zoom) * 300, 1e-6)
        // 화면이 넓을수록 더 확대한다.
        assertTrue(zoomFor(metersPerDpAtZoom0, 600f) > zoom)
    }
}
