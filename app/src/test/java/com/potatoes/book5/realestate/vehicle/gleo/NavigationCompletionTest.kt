package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.vehicle.nav.NavigationCompletion
import org.junit.Assert.*
import org.junit.Test

class NavigationCompletionTest {
    @Test fun delayedGuidanceRetainsCorrelationWithoutExtendingSubmissionFreshness() {
        val f=TourFixture();val context=f.context()
        val completion=NavigationCompletion(context.requestId) { f.access.observationValid(context) && context.voice?.visualDetailsAllowed==true }
        f.clock.at=f.clock.at.plusSeconds(3)
        assertFalse(f.access.valid(context,true))
        val result=Outcome.Success(NavigationReceipt("GUIDING",context.requestId))
        assertEquals(NavigationEvent(context.requestId,result),completion.finish(result))
        assertNull(completion.finish(Outcome.Failure(DomainError.Timeout)))
    }
    @Test fun timeoutAfterTenSecondsStaysTimeoutAndIgnoresLateGuidance() {
        val f=TourFixture();val context=f.context()
        val completion=NavigationCompletion(context.requestId) { f.access.observationValid(context) && context.voice?.visualDetailsAllowed==true }
        f.clock.at=f.clock.at.plusSeconds(10)
        assertEquals(Outcome.Failure(DomainError.Timeout),completion.finish(Outcome.Failure(DomainError.Timeout))!!.result)
        assertNull(completion.finish(Outcome.Success(NavigationReceipt("GUIDING",context.requestId))))
    }
    @Test fun revokedContextAndCancelledRequestCannotProduceSuccessfulTerminalEvents() {
        val f=TourFixture();val context=f.context()
        val completion=NavigationCompletion(context.requestId) { f.access.observationValid(context) }
        f.voices.setConsented(false)
        assertTrue(completion.finish(Outcome.Success(NavigationReceipt("GUIDING",context.requestId)))!!.result is Outcome.Failure)
        val cancelled=NavigationCompletion("other") { true };cancelled.cancel()
        assertNull(cancelled.finish(Outcome.Success(NavigationReceipt("GUIDING","other"))))
    }
}
