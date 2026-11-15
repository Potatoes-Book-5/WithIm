package com.potatoes.book5.realestate.vehicle.gleo

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.potatoes.book5.realestate.GleoActionReceiver
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class GleoWireTest {
    private val decoder = GleoIntentDecoder()
    private val price = GleoAction.GET_AREA_PRICE_SUMMARY
    @Test fun decoderAcceptsOnlyKnownActionAndSelectedJsonProfile() {
        assertNull(decoder.decode(Intent("UNKNOWN")))
        val valid = decoder.decode(Intent(price.name).putExtra("ACTION_PARAM", "{\"target\":\"CURRENT_LOCATION\"}"))!!
        assertNull(valid.decodingError)
        assertEquals("CURRENT_LOCATION", valid.parameters["target"])
        assertEquals("MALFORMED_REQUEST", decoder.decode(Intent(price.name).putExtra("ACTION_PARAM", "[1]"))!!.decodingError)
        assertEquals("MALFORMED_REQUEST", decoder.decode(Intent(price.name).putExtra("ACTION_PARAM", "not JSON"))!!.decodingError)
        assertEquals("CONFLICTING_WIRE_PROFILE", decoder.decode(Intent(price.name)
            .putExtra("ACTION_PARAM", "{}").putExtra("actionParameters", "{}"))!!.decodingError)
    }
    @Test fun decoderAcceptsFlatExtrasAndRejectsMixedProfile() {
        assertEquals("CURRENT_LOCATION", decoder.decode(Intent(price.name).putExtra("target", "CURRENT_LOCATION"))!!.parameters["target"])
        assertEquals("CONFLICTING_WIRE_PROFILE", decoder.decode(Intent(price.name).putExtra("target", "CURRENT_LOCATION").putExtra("ACTION_PARAM", "{}"))!!.decodingError)
    }
    @Test fun implicitFlatRequestResolvesAndReturnsCallback() {
        withResponseReceiver { context, responses ->
            GleoAction.entries.forEach { action ->
                val matches = context.packageManager.queryBroadcastReceivers(Intent(action.name).setPackage(context.packageName), 0)
                assertTrue(matches.any { it.activityInfo.name == GleoActionReceiver::class.java.name })
            }
            context.sendBroadcast(Intent(price.name).setPackage(context.packageName).putExtra("target", "CURRENT_LOCATION"))
            assertPublicLocationRejected(take(responses))
        }
    }
    @Test fun decoderBoundsPayloadAndDoesNotCoerceValues() {
        assertEquals("PAYLOAD_TOO_LARGE", decoder.decode(Intent(price.name).putExtra("ACTION_PARAM", "a".repeat(4097)))!!.decodingError)
        val wrongType = decoder.decode(Intent(price.name).putExtra("ACTION_PARAM", "{\"target\":42}"))!!
        assertEquals(GleoValidation.Invalid("INVALID_TARGET_TYPE"), GleoRequestValidator().validate(wrongType))
        val nullType = decoder.decode(Intent(price.name).putExtra("ACTION_PARAM", "{\"target\":null}"))!!
        assertEquals(GleoValidation.Invalid("INVALID_TARGET_TYPE"), GleoRequestValidator().validate(nullType))
    }
    @Test fun callbackEncodesNestedDataAsStringAndAlwaysStaysSilent() {
        val json = JSONObject(GleoCallbackEncoder().encode(GleoCallback.failure(price, "INVALID_REQUEST")))
        assertEquals(price.name, json.getString("functionName"))
        assertEquals("silent", json.getString("mode"))
        assertTrue(json.get("data") is String)
        assertEquals("INVALID_REQUEST", JSONArray(json.getString("data")).getJSONObject(0).getString("code"))
    }
    @Test fun manifestReceiverDispatchesThreeActionsThroughApplicationContainer() {
        withResponseReceiver { context, responses ->
            send(context, GleoAction.GET_AREA_PRICE_SUMMARY, "{\"target\":\"CURRENT_LOCATION\"}")
            val priceResponse = take(responses)
            assertPublicLocationRejected(priceResponse)
            assertEquals("silent", priceResponse.getString("mode"))

            send(context, GleoAction.SAVE_INTEREST_AREA, "{\"target\":\"CURRENT_LOCATION\"}")
            val save = take(responses)
            assertEquals("fail", save.getString("status"))
            val mutation = JSONArray(save.getString("data")).getJSONObject(0)
            assertEquals("UNSUPPORTED", mutation.getString("code"))
            assertFalse(mutation.getBoolean("synthetic"))

            send(context, GleoAction.GET_REAL_ESTATE_BRIEFING, "{}")
            val briefing = take(responses)
            assertEquals("fail", briefing.getString("status"))
            val unsupported = JSONArray(briefing.getString("data")).getJSONObject(0)
            assertEquals("UNSUPPORTED", unsupported.getString("code"))
            assertFalse(unsupported.getBoolean("synthetic"))
            assertNull(responses.poll(250, TimeUnit.MILLISECONDS))
        }
    }
    @Test fun receiverRejectsMalformedAndMissingTargetThenRemainsUsable() {
        withResponseReceiver { context, responses ->
            send(context, GleoAction.SAVE_INTEREST_AREA, "bad JSON")
            assertEquals("fail", take(responses).getString("status"))
            send(context, GleoAction.SAVE_INTEREST_AREA, "{}")
            assertEquals("asking", take(responses).getString("status"))
            send(context, GleoAction.GET_AREA_PRICE_SUMMARY, "{\"target\":\"CURRENT_LOCATION\"}")
            assertPublicLocationRejected(take(responses))
        }
    }
    @Test fun receiverIgnoresUnknownAction() {
        withResponseReceiver { context, responses ->
            context.sendBroadcast(Intent("UNKNOWN").setComponent(ComponentName(context, GleoActionReceiver::class.java)))
            assertNull(responses.poll(500, TimeUnit.MILLISECONDS))
        }
    }
    private fun assertPublicLocationRejected(response: JSONObject) {
        assertEquals("fail", response.getString("status"))
        val data = JSONArray(response.getString("data")).getJSONObject(0)
        assertEquals("UNSUPPORTED", data.getString("code"))
        assertFalse(data.getBoolean("synthetic"))
        assertEquals("UNAVAILABLE", data.getString("executionMode"))
    }
    private fun send(context: Context, action: GleoAction, json: String) {
        context.sendBroadcast(Intent(action.name).setComponent(ComponentName(context, GleoActionReceiver::class.java))
            .putExtra("ACTION_PARAM", json))
    }
    private fun take(queue: LinkedBlockingQueue<Intent>): JSONObject {
        val intent = queue.poll(10, TimeUnit.SECONDS)
        assertNotNull("Expected one Gleo response callback", intent)
        assertEquals(InstrumentationRegistry.getInstrumentation().targetContext.packageName, intent!!.getStringExtra("packageName"))
        return JSONObject(intent.getStringExtra("response")!!)
    }
    private fun withResponseReceiver(test: (Context, LinkedBlockingQueue<Intent>) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val queue = LinkedBlockingQueue<Intent>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { queue.offer(intent) }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(BroadcastGleoCallbackSender.GLEO_RESPONSE_ACTION), ContextCompat.RECEIVER_EXPORTED)
        try { test(context, queue) } finally { context.unregisterReceiver(receiver) }
    }
}
