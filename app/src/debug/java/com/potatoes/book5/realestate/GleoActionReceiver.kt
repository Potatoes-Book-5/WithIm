package com.potatoes.book5.realestate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.concurrent.atomic.AtomicBoolean

/** Composition-root bootstrap. Parsing, dispatch and response mapping belong to vehicle.gleo. */
class GleoActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val receivedAtMillis = System.nanoTime() / 1_000_000L
        val pending = goAsync()
        val finished = AtomicBoolean(false)
        fun finish() { if (finished.compareAndSet(false, true)) pending.finish() }
        val app = context.applicationContext as? RealEstateApplication
        if (app == null) { finish(); return }
        try {
            // Completion handlers also run if cancellation happens before the coroutine body starts.
            app.launchGleoRequest(intent, receivedAtMillis).invokeOnCompletion { finish() }
        } catch (_: Exception) {
            finish()
        }
    }
}
