package com.potatoes.book5.realestate.vehicle.gleo

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull

/** Owns the accepted request only. Overlaps are dropped without an ambiguous function-only response. */
class GleoRequestCoordinator(
    private val apiBudgetMillis: Long = 4_000L,
    private val totalBudgetMillis: Long = 5_000L,
    private val nowMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private val active = AtomicBoolean(false)
    init { require(apiBudgetMillis > 0 && totalBudgetMillis > apiBudgetMillis) }

    suspend fun execute(
        action: GleoAction,
        sender: GleoCallbackSender,
        receivedAtMillis: Long? = null,
        operation: suspend () -> GleoCallback,
    ) {
        if (!active.compareAndSet(false, true)) return
        val started = receivedAtMillis ?: nowMillis()
        try {
            val available = totalBudgetMillis - (nowMillis() - started)
            if (available <= 0) return
            val apiTime = minOf(apiBudgetMillis, available - (totalBudgetMillis - apiBudgetMillis))
            val response = (if (apiTime > 0) withTimeoutOrNull(apiTime) { operation() } else null)
                ?: GleoCallback.failure(action, "TIMEOUT")
            currentCoroutineContext().ensureActive()
            val remaining = totalBudgetMillis - (nowMillis() - started)
            if (remaining > 0) withTimeoutOrNull(remaining) {
                try { sender.send(response) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { /* Transport failure: never retry a possibly delivered callback. */ }
            }
        } finally {
            active.set(false)
        }
    }
}
