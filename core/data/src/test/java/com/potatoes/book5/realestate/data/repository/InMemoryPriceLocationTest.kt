package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.port.*
import java.time.Instant
import java.time.Clock
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class InMemoryPriceLocationTest {
    private val at = Instant.parse("2026-11-15T03:00:00Z")
    private val fix = VehicleLocationState(VehiclePose(GeoPoint(37.25,127.05)),10.0,at,LocationStatus.AVAILABLE)
    private val area = MarketDistrict("41117","경기도 수원시 영통구")
    private fun store()=InMemoryPriceLocation(Clock.fixed(at.plusSeconds(2),ZoneOffset.UTC))
    @Test fun needsActualFixAndMatchingBackgroundResolution() {
        val store=store()
        assertNull(store.snapshot().area)
        store.publish(fix,area,store.revision())
        assertNull(store.snapshot().area)
        store.updateFix(fix);store.publish(fix,area,store.revision())
        assertEquals("41117",store.snapshot().area?.id)
        assertEquals(at,store.snapshot().observedAt)
    }
    @Test fun permissionInterruptionRejectsAnOldAsyncResolutionEvenAfterRecovery() {
        val store=store();store.updateFix(fix);val request=store.revision()
        store.updateFix(VehicleLocationState(status=LocationStatus.CONSENT_REQUIRED))
        store.updateFix(fix)
        store.publish(fix,area,request)
        assertNull(store.snapshot().area)
    }
    @Test fun sourceSwitchAndLargeMovementDoNotReuseTheOldPoint() {
        val store=store();store.updateFix(fix);store.publish(fix,area,store.revision())
        store.updateFix(fix.copy(pose=VehiclePose(GeoPoint(38.0,128.0))))
        assertNull(store.snapshot().area)
        store.updateFix(fix.copy(source=LocationSource.EMULATOR_ROUTE))
        assertNull(store.snapshot().area)
    }
    @Test fun newestResolvedDistrictWinsWithoutRankingDwellOrOlderReplyRollback() {
        val store=store();store.updateFix(fix);val request=store.revision()
        val newer=fix.copy(observedAt=at.plusSeconds(1))
        store.updateFix(newer);store.publish(newer,MarketDistrict("11140","서울특별시 중구"),request)
        store.publish(fix,area,request)
        assertEquals("11140",store.snapshot().area?.id)
    }
}
