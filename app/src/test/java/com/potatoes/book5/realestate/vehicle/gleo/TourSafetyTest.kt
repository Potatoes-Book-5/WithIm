package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.model.tour.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TourSafetyTest {
    @Test fun duplicateSaveAndProfileIsolationUseActualResolvedArea() = runTest {
        val f=TourFixture();f.prepare(this)
        val first=f.save(f.area.name,f.context())
        val second=f.save(f.area.name,f.context())
        assertFalse(((first as Outcome.Success).value as ActionResult.Completed).payload.alreadySaved)
        assertTrue(((second as Outcome.Success).value as ActionResult.Completed).payload.alreadySaved)
        assertEquals(1,f.store.writes)
        assertTrue((f.store.areas("other") as Outcome.Success).value.isEmpty())
    }
    @Test fun driveSignalWaitBackgroundAndUntrustedCallsCannotExecutePersonalActions() = runTest {
        val f=TourFixture();f.prepare(this)
        f.voices.setAppState(true,"a",visualDetailsAllowed=false)
        for(intent in listOf(TourIntent.SaveArea(f.area.name),TourIntent.CheckArea(f.area.name),TourIntent.Briefing,TourIntent.Analytics(),TourIntent.Navigate(f.entrance),TourIntent.CameraScan()))
            assertTrue(f.adapter.handle(intent,f.context()) is Outcome.Failure)
        assertEquals(0,f.store.writes);assertEquals(0,f.navigator.calls);assertEquals(0,f.camera.calls)
        f.voices.setAppState(true,"a",visualDetailsAllowed=true)
        assertTrue(f.adapter.handle(TourIntent.SaveArea(f.area.name),f.context().copy(trusted=false)) is Outcome.Failure)
        f.voices.setForeground(false)
        assertTrue(f.adapter.handle(TourIntent.SaveArea(f.area.name),f.context()) is Outcome.Failure)
    }
    @Test fun unknownEntranceCentroidUnknownRouteAndNavigationFailureNeverClaimGuidance() = runTest {
        val f=TourFixture();f.prepare(this)
        val missing=f.adapter.handle(TourIntent.Navigate(null),f.context()) as Outcome.Success
        assertEquals(ActionResult.NeedsInput("entrance"),missing.value)
        assertTrue(f.adapter.handle(TourIntent.Navigate(f.entrance.copy(evidence=EntranceEvidence.UNVERIFIED)),f.context()) is Outcome.Failure)
        f.navigator.active=null
        assertEquals(ActionResult.NeedsInput("replaceRoute"),(f.adapter.handle(TourIntent.Navigate(f.entrance),f.context()) as Outcome.Success).value)
        assertEquals(0,f.navigator.calls)
        f.navigator.result=Outcome.Failure(DomainError.Offline)
        assertEquals(Outcome.Failure(DomainError.Offline),f.adapter.handle(TourIntent.Navigate(f.entrance,true),f.context()))
        assertEquals(1,f.navigator.calls)
    }
    @Test fun noLocationNoSavedAreaAndAmbiguousPlaceDoNotBecomePositiveMembership() = runTest {
        val f=TourFixture();f.prepare(this)
        assertEquals(ActionResult.NeedsInput("interestArea"),(f.check(null,f.context()) as Outcome.Success).value)
        f.location.updateFix(com.potatoes.book5.realestate.domain.port.VehicleLocationState())
        assertTrue(f.check(f.area.name,f.context()) is Outcome.Failure)
    }
    @Test fun exclusiveBudgetUpperBoundaryAndMissingHouseholdsAreNotReadyMatches() = runTest {
        val f=TourFixture();f.prepare(this)
        f.condition=InterestCondition(priceRanges=setOf(PriceRange.FROM_5_TO_10))
        val now=f.clock.instant();val today=java.time.LocalDate.now(f.clock)
        val c=TourCandidate("x","X","41117","dong",f.center,PriceRecord(100_000,today),25,null,2001,now)
        f.market.publishMarket(TourMarket("41117",listOf(c),now,priceLeaderId="x"),f.market.currentRevision())
        assertTrue((f.recommend(f.area) as Outcome.Success).value.first.isEmpty())
        f.condition=InterestCondition(minHouseholds=HouseholdMin.OVER_300)
        val incomplete=(f.recommend(f.area) as Outcome.Success).value
        assertTrue(incomplete.first.isEmpty());assertEquals("PARTIAL_METADATA",incomplete.third)
        f.market.publishAnalytics(f.detail.copy(id="x",households=500),now)
        val ready=(f.recommend(f.area) as Outcome.Success).value
        assertEquals("READY",ready.third);assertEquals("x",ready.first.single().apartment.id)
    }
    @Test fun deletionDuringRecommendationInvalidatesSavedAndEntryFacts() = runTest {
        val f=TourFixture();f.prepare(this);f.saved()
        f.onInterestRead={ f.store.deleteArea("p",f.area.id) }
        assertTrue(f.check(f.area.name,f.context()) is Outcome.Failure)
    }
    @Test fun outdatedBriefingReceiptsDoNotMarkAReplacementVersionRead() = runTest {
        val f=TourFixture();f.prepare(this);f.saved();f.seedBriefing("v2")
        val old=f.adapter.handle(TourIntent.ConfirmBriefing(listOf(BriefingReceipt(f.area.id,"v1"))),f.context()) as Outcome.Success
        assertEquals(TourReply.Confirmed(0),(old.value as ActionResult.Completed).payload)
        assertNull(f.store.briefings.single().readVersion)
    }
    @Test fun revokedContextDuringNavigationConnectionDoesNotSubmitAndPendingRequestBlocksNext() = runTest {
        val f=TourFixture();f.prepare(this)
        f.navigator.isPending=true
        assertTrue(f.adapter.handle(TourIntent.Navigate(f.entrance),f.context()) is Outcome.Failure)
        assertEquals(0,f.navigator.calls)
        f.navigator.isPending=false
        f.navigator.onConnect={ f.voices.setConsented(false) }
        assertTrue(f.adapter.handle(TourIntent.Navigate(f.entrance),f.context()) is Outcome.Failure)
        assertEquals(0,f.navigator.calls)
    }
    @Test fun revokedConfirmationDoesNotAcknowledgeAndDeletedRegionCannotReturnBriefing() = runTest {
        val f=TourFixture();f.prepare(this);f.saved();f.seedBriefing()
        f.store.onAcknowledge={ f.voices.setConsented(false) }
        assertTrue(f.adapter.handle(TourIntent.ConfirmBriefing(listOf(BriefingReceipt(f.area.id,"v1"))),f.context()) is Outcome.Failure)
        assertNull(f.store.briefings.single().readVersion)
        f.voices.setConsented(true)
        f.store.onSourceStates={ f.store.deleteArea("p",f.area.id) }
        assertTrue(f.briefing.get(f.context()) is Outcome.Failure)
    }
}
