package com.potatoes.book5.realestate.vehicle.gleo

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GleoRequestCoordinatorTest {
    private val action = GleoAction.GET_AREA_PRICE_SUMMARY
    private fun response() = GleoCallback.failure(action, "TEST")
    @Test fun timeoutProducesExactlyOneResponseAndNoLateSuccess() = runTest {
        val coordinator = GleoRequestCoordinator(nowMillis = { currentTime })
        val sent = mutableListOf<GleoCallback>()
        coordinator.execute(action, { sent += it }) { delay(5_000); response() }
        assertEquals(4_000L, currentTime)
        assertEquals(1, sent.size)
        assertEquals("TIMEOUT", sent.single().data.single()["code"])
        advanceUntilIdle()
        assertEquals(1, sent.size)
    }
    @Test fun overlappingRequestDoesNotSendAmbiguousBusyResponse() = runTest {
        val coordinator = GleoRequestCoordinator(nowMillis = { currentTime })
        val sent = mutableListOf<GleoCallback>()
        var calls = 0
        val first = launch { coordinator.execute(action, { sent += it }) { calls++; delay(100); response() } }
        runCurrent()
        coordinator.execute(action, { sent += it }) { calls++; response() }
        first.join()
        assertEquals(1, calls)
        assertEquals(1, sent.size)
    }
    @Test fun cancelledRequestSendsNothingAndReleasesSingleFlight() = runTest {
        val coordinator = GleoRequestCoordinator(nowMillis = { currentTime })
        val sent = mutableListOf<GleoCallback>()
        val first = launch { coordinator.execute(action, { sent += it }) { delay(10_000); response() } }
        runCurrent(); first.cancelAndJoin()
        assertTrue(sent.isEmpty())
        coordinator.execute(action, { sent += it }) { response() }
        assertEquals(1, sent.size)
    }
    @Test fun expiredAtReceiptDoesNotInvokeApiOrRespond() = runTest {
        val coordinator = GleoRequestCoordinator(nowMillis = { currentTime })
        var invoked = false
        var sent = false
        coordinator.execute(action, { sent = true }, receivedAtMillis = -5_000) { invoked = true; response() }
        assertFalse(invoked)
        assertFalse(sent)
    }
    @Test fun completedAfterDeadlineIsDiscarded() = runTest {
        var clock = 0L
        val coordinator = GleoRequestCoordinator(nowMillis = { clock })
        var sent = false
        coordinator.execute(action, { sent = true }) { clock = 5_001; response() }
        assertFalse(sent)
    }
    @Test fun sendFailureIsNotRetriedAndDoesNotRetainGate() = runTest {
        val coordinator = GleoRequestCoordinator(nowMillis = { currentTime })
        var attempts = 0
        coordinator.execute(action, { attempts++; error("transport failure") }) { response() }
        coordinator.execute(action, { attempts++ }) { response() }
        assertEquals(2, attempts)
    }
    @Test fun senderTimeIsIncludedInOverallBudget() = runTest {
        val coordinator = GleoRequestCoordinator(nowMillis = { currentTime })
        var completed = false
        coordinator.execute(action, { delay(6_000); completed = true }) { response() }
        assertEquals(5_000L, currentTime)
        assertFalse(completed)
    }
}
