package com.potatoes.book5.realestate.vehicle.gleo

import android.content.Intent
import org.json.JSONObject

/** Explicit wire profiles: object JSON ACTION_PARAM or flat extras; mixed profiles are rejected. */
class GleoIntentDecoder {
    fun decode(intent: Intent): GleoInput? {
        val action = GleoAction.fromWire(intent.action) ?: return null
        return try {
            if (intent.hasExtra("actionParameters")) return GleoInput(action, decodingError = "CONFLICTING_WIRE_PROFILE")
            val flatKeys = listOf("target", "areaQuery").filter { intent.hasExtra(it) }
            if (intent.hasExtra("ACTION_PARAM") && flatKeys.isNotEmpty())
                return GleoInput(action, decodingError = "CONFLICTING_WIRE_PROFILE")
            if (!intent.hasExtra("ACTION_PARAM")) {
                val parameters = flatKeys.associateWith { intent.extras?.get(it) }
                if (parameters.values.any { it is String && it.length > 256 })
                    return GleoInput(action, decodingError = "PAYLOAD_TOO_LARGE")
                return GleoInput(action, parameters)
            }
            val raw = intent.getStringExtra("ACTION_PARAM")
                ?: return GleoInput(action, decodingError = "INVALID_WIRE_TYPE")
            if (raw.length > 4_096) return GleoInput(action, decodingError = "PAYLOAD_TOO_LARGE")
            val json = JSONObject(raw)
            val parameters = json.keys().asSequence().associateWith { key -> json.get(key).takeUnless { it == JSONObject.NULL } }
            GleoInput(action, parameters)
        } catch (_: Exception) {
            GleoInput(action, decodingError = "MALFORMED_REQUEST")
        }
    }
}
class GleoAndroidHandler(
    private val adapter: GleoIntentAdapter,
    private val decoder: GleoIntentDecoder = GleoIntentDecoder(),
) {
    fun captureContext() = adapter.captureContext()
    suspend fun handle(intent: Intent, receivedAtMillis: Long = System.nanoTime() / 1_000_000L,
        context: com.potatoes.book5.realestate.domain.model.appaction.AppCallContext? = null) {
        decoder.decode(intent)?.let { adapter.handle(it.copy(receivedAtMillis = receivedAtMillis), context) }
    }
}
