package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.model.tour.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TourEntryTest {
    private fun atMeters(f:TourFixture,meters:Double)=GeoPoint(f.center.latitude+Math.toDegrees(meters/6_371_008.8),f.center.longitude)
    private suspend fun move(f:TourFixture,meters:Double,accuracy:Double?=10.0,session:String="s"): List<AreaEntry> {
        f.clock.at=f.clock.at.plusSeconds(1);f.fix(atMeters(f,meters),accuracy)
        return (f.entries(f.context(session=session)) as Outcome.Success).value
    }
    @Test fun gradualCrossingAndAccuracyBandKeepArmingAndQueryDoesNotMutateIt() = runTest {
        val f=TourFixture();f.prepare(this);f.saved()
        assertTrue(move(f,650.0).isEmpty())
        assertTrue(move(f,550.0).isEmpty())
        assertTrue(move(f,500.0).isEmpty()) // Uncertain, not a lost outer observation.
        val entered=move(f,450.0)
        assertEquals(1,entered.size)
        val checked=(f.check(f.area.name,f.context()) as Outcome.Success).value as ActionResult.Completed
        assertEquals(entered.single(),checked.payload.entry)
        assertEquals(1,f.store.recorded.size)
        f.check(f.area.name,f.context());assertEquals(1,f.store.recorded.size)
    }
    @Test fun firstInsideAndColdSessionDoNotInventAnEntry() = runTest {
        val f=TourFixture();f.prepare(this);f.saved()
        assertTrue(move(f,100.0).isEmpty())
        assertNull(f.entries.current("p",f.area.id,"s"))
        move(f,650.0)
        assertTrue(move(f,100.0,session="new-session").isEmpty())
    }
    @Test fun foregroundResumeAndDeleteResaveResetEarlierArmingAndEvidence() = runTest {
        val f=TourFixture();f.prepare(this);f.saved();move(f,650.0)
        f.voices.setForeground(false);f.voices.setForeground(true)
        assertTrue(move(f,100.0).isEmpty())
        move(f,650.0)
        f.store.deleteArea("p",f.area.id);f.saved()
        assertTrue(move(f,100.0).isEmpty())
        move(f,650.0);assertEquals(1,move(f,100.0).size)
        f.store.deleteArea("p",f.area.id);f.saved()
        val check=(f.check(f.area.name,f.context()) as Outcome.Success).value as ActionResult.Completed
        assertNull(check.payload.entry)
    }
    @Test fun cooldownSurvivesASecondVisitAndNoBoundaryJitterDuplicates() = runTest {
        val f=TourFixture();f.prepare(this);f.saved();move(f,650.0);assertEquals(1,move(f,100.0).size)
        move(f,550.0);move(f,450.0);assertEquals(1,f.store.recorded.size)
        move(f,650.0);assertTrue(move(f,100.0).isEmpty())
        f.clock.at=f.clock.at.plusSeconds(600)
        move(f,650.0);assertEquals(1,move(f,100.0).size)
        assertEquals(2,f.store.recorded.size)
    }
    @Test fun missingAccuracyAndLongObservationGapCannotCreateAVisit() = runTest {
        val f=TourFixture();f.prepare(this);f.saved();move(f,650.0)
        assertTrue(move(f,500.0,null).isEmpty())
        assertTrue(move(f,100.0).isEmpty())
        move(f,650.0);f.clock.at=f.clock.at.plusSeconds(11)
        assertTrue(move(f,100.0).isEmpty())
    }
}
