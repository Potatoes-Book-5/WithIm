package com.potatoes.book5.realestate.probe

import android.app.Activity
import androidx.test.runner.AndroidJUnitRunner
import android.content.*
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.potatoes.book5.realestate.RealEstateApplication
import com.potatoes.book5.realestate.vehicle.gleo.BroadcastGleoCallbackSender
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import com.potatoes.book5.realestate.domain.error.DomainError
import org.json.JSONArray
import org.json.JSONObject

/** Opt-in, real provider smoke. Uses the unmodified app DI; never seeds prices or changes consent. */
class RealEstateTestRunner : AndroidJUnitRunner() {
    private var liveProbe = false
    override fun onCreate(arguments: Bundle?) {
        liveProbe = arguments?.getString("livePriceProbe") == "true"
        super.onCreate(arguments)
    }
    override fun onStart() {
        if (!liveProbe) { super.onStart(); return }
        val report = Bundle().apply { putString("probeStatus", "BLOCKED") }
        try {
            runBlocking { withTimeout(65_000) { probe(report) } }
        } catch (error: Exception) {
            report.putString("probeReason", if (error is TimeoutCancellationException) "DEADLINE" else "PROBE_ERROR")
        }
        finish(if (report.getString("probeStatus") == "PASS") Activity.RESULT_OK else Activity.RESULT_CANCELED, report)
    }

    private suspend fun probe(report: Bundle) {
        targetContext.startActivity(Intent().setClassName(targetContext.packageName,
            "com.potatoes.book5.realestate.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val app = targetContext.applicationContext as RealEstateApplication
        val container = app.container
        val handler = container.gleoIntentHandler ?: run { report.putString("probeReason", "HANDLER_UNAVAILABLE"); return }
        val deadline = android.os.SystemClock.elapsedRealtime() + 45_000
        var area = container.preparedMarket.knownDistricts().firstOrNull()
        while ((area == null || !container.voiceContext.snapshot().let { it.foreground && it.enabled }) &&
            android.os.SystemClock.elapsedRealtime() < deadline) {
            delay(500)
            area = container.preparedMarket.knownDistricts().firstOrNull()
        }
        report.putBoolean("probeMarketPrepared", area != null)
        if (area == null) {
            report.putString("probeReason", "MARKET_NOT_READY")
            val error = withTimeoutOrNull(2_000) { container.marketRepository.observeNearbyRanking().first().error }
            report.putString("probeProviderState", when (error) {
                null -> "PENDING"
                DomainError.MissingApiKey -> "SERVICE_UNAVAILABLE"
                is DomainError.Http -> "HTTP_" + error.statusCode
                DomainError.ProviderRejected -> "PROVIDER_REJECTED"
                DomainError.Offline -> "OFFLINE"
                DomainError.Timeout -> "TIMEOUT"
                else -> "UNAVAILABLE"
            })
            return
        }
        // Respect existing onboarding and consent, even in a disposable instance.
        val voice = container.voiceContext.snapshot()
        if (!voice.foreground || !voice.enabled) { report.putString("probeReason", "CONTEXT_NOT_READY"); return }
        val responses = LinkedBlockingQueue<Intent>()
        val receiver = object : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra("packageName") == targetContext.packageName) responses.offer(intent)
        } }
        ContextCompat.registerReceiver(targetContext, receiver,
            IntentFilter(BroadcastGleoCallbackSender.GLEO_RESPONSE_ACTION), ContextCompat.RECEIVER_EXPORTED)
        try {
            val captured = handler.captureContext()
            val params = JSONObject().put("target", "NAMED_AREA").put("areaQuery", area.name)
            handler.handle(Intent("GET_AREA_PRICE_SUMMARY").putExtra("ACTION_PARAM", params.toString()), context = captured)
            val response = responses.poll(8, TimeUnit.SECONDS)?.getStringExtra("response")
            if (response == null) { report.putString("probeReason", "NO_CALLBACK"); return }
            val root = JSONObject(response)
            val data = JSONArray(root.getString("data")).getJSONObject(0)
            report.putBoolean("probeCallback", true)
            if (root.getString("status") != "success") {
                report.putString("probeReason", "ACTION_UNAVAILABLE"); return
            }
            val count = data.getInt("sampleCount")
            val valid = data.getString("executionMode") == "LIVE" && !data.getBoolean("synthetic") &&
                data.getString("sourceId") == "molit-apartment-sale" && data.getString("scope") == "DISTRICT" && count >= 0 &&
                (if (count == 0) data.isNull("averagePriceKrw") && data.isNull("highestPriceKrw")
                else data.getLong("averagePriceKrw") > 0 && data.getLong("highestPriceKrw") >= data.getLong("averagePriceKrw"))
            report.putString("probeStatus", if (valid) "PASS" else "FAIL")
            report.putString("probeReason", if (valid) "REAL_PROVIDER_CALLBACK" else "INVALID_RESULT")
            report.putInt("probeSamples", count)
            report.putBoolean("probeSynthetic", data.getBoolean("synthetic"))
            // Do not emit prices, names, location, raw callback data or credentials.
        } finally { targetContext.unregisterReceiver(receiver) }
    }
}
