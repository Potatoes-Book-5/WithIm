package com.potatoes.book5.realestate.vehicle.gear

import ai.pleos.playground.vehicle.constant.control.VehicleGear
import com.potatoes.book5.realestate.domain.model.GearPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleGearMappingTest {

    @Test
    fun `SDK 기어 값을 도메인 기어로 바꾼다`() {
        assertEquals(GearPosition.PARK, VehicleGear.GEAR_PARK.toDomain())
        assertEquals(GearPosition.REVERSE, VehicleGear.GEAR_REVERSE.toDomain())
        assertEquals(GearPosition.NEUTRAL, VehicleGear.GEAR_NEUTRAL.toDomain())
        assertEquals(GearPosition.DRIVE, VehicleGear.GEAR_DRIVE.toDomain())
        assertEquals(GearPosition.UNKNOWN, VehicleGear.GEAR_UNKNOWN.toDomain())
    }

    @Test
    fun `수동 단수는 모두 MANUAL이다`() {
        val manual = VehicleGear.entries.filter { it.name.removePrefix("GEAR_").toIntOrNull() != null }
        assertEquals(9, manual.size)
        manual.forEach { assertEquals(it.name, GearPosition.MANUAL, it.toDomain()) }
    }
}
