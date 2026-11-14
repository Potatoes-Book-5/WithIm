package com.potatoes.book5.realestate.vehicle.gear

import com.potatoes.book5.realestate.domain.model.GearPosition
import org.junit.Assert.assertEquals
import org.junit.Test

/** AAOS `VehicleGear` 값(docs: developer.android.com android.car.VehicleGear)을 도메인 기어로 바꾼다. */
class GearPositionMappingTest {

    @Test
    fun `P R N D를 도메인 기어로 바꾼다`() {
        assertEquals(GearPosition.PARK, gearPositionOf(4))
        assertEquals(GearPosition.REVERSE, gearPositionOf(2))
        assertEquals(GearPosition.NEUTRAL, gearPositionOf(1))
        assertEquals(GearPosition.DRIVE, gearPositionOf(8))
    }

    @Test
    fun `1단부터 9단은 수동 단수, 그 밖의 값은 알 수 없음이다`() {
        listOf(16, 32, 64, 128, 256, 512, 1024, 2048, 4096).forEach { assertEquals(GearPosition.MANUAL, gearPositionOf(it)) }
        assertEquals(GearPosition.UNKNOWN, gearPositionOf(0))
        assertEquals(GearPosition.UNKNOWN, gearPositionOf(3))
    }
}
