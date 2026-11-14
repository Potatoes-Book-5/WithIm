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

/** DI selects the documented Gleo route for debug integration; unit fixtures may use a local sink. */
class BroadcastGleoCallbackSender(
    context: Context,
    private val responseAction: String = TEST_RESPONSE_ACTION,
    private val encoder: GleoCallbackEncoder = GleoCallbackEncoder(),
) : GleoCallbackSender {
    private val appContext = context.applicationContext
    override suspend fun send(callback: GleoCallback) {
        val intent = Intent(responseAction)
        if (responseAction == TEST_RESPONSE_ACTION) intent.setPackage(appContext.packageName)
        appContext.sendBroadcast(intent
            .putExtra("packageName", appContext.packageName)
            .putExtra("response", encoder.encode(callback)))
    }
    companion object { const val GLEO_RESPONSE_ACTION = "ai.pleos.playground.intent.action.APP_ACTION_RESPONSE"
        const val TEST_RESPONSE_ACTION = "com.potatoes.book5.realestate.debug.GLEO_DUMMY_RESPONSE" }
}
