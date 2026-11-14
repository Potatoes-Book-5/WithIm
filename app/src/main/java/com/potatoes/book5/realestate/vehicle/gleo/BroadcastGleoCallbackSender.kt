package com.potatoes.book5.realestate.vehicle.gleo

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

class GleoCallbackEncoder {
    fun encode(callback: GleoCallback): String = JSONObject().apply {
        put("functionName", callback.action.name)
        put("status", callback.status)
        put("message", callback.message)
        put("mode", "silent")
        put("data", JSONArray().apply { callback.data.forEach { put(JSONObject(it)) } }.toString())
    }.toString()
}

/** Fixed app-local test sink, deliberately not the unverified production Gleo response route. */
class BroadcastGleoCallbackSender(
    context: Context,
    private val encoder: GleoCallbackEncoder = GleoCallbackEncoder(),
) : GleoCallbackSender {
    private val appContext = context.applicationContext
    override suspend fun send(callback: GleoCallback) {
        appContext.sendBroadcast(Intent(TEST_RESPONSE_ACTION).setPackage(appContext.packageName)
            .putExtra("packageName", appContext.packageName)
            .putExtra("response", encoder.encode(callback)))
    }
    companion object { const val TEST_RESPONSE_ACTION = "com.potatoes.book5.realestate.debug.GLEO_DUMMY_RESPONSE" }
}
