package com.potatoes.book5.realestate.vehicle.gleo

import android.content.Intent
import org.json.JSONObject

/** Test wire profile only: ACTION_PARAM contains one JSON object, never an encoded JSON string. */
class GleoIntentDecoder {
    fun decode(intent: Intent): GleoInput? {
        val action = GleoAction.fromWire(intent.action) ?: return null
        return try {
            if (intent.hasExtra("actionParameters")) return GleoInput(action, decodingError = "CONFLICTING_WIRE_PROFILE")
            if (!intent.hasExtra("ACTION_PARAM")) return GleoInput(action)
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
    suspend fun handle(intent: Intent, receivedAtMillis: Long = System.nanoTime() / 1_000_000L) {
        decoder.decode(intent)?.let { adapter.handle(it.copy(receivedAtMillis = receivedAtMillis)) }
    }
}
