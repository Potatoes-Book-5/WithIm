package com.potatoes.book5.realestate.vehicle.gleo

import android.content.*
import androidx.core.content.ContextCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.potatoes.book5.realestate.data.repository.*
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.port.*
import com.potatoes.book5.realestate.domain.usecase.*
import java.math.BigDecimal
import java.time.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Production collector -> prepared model -> real price API -> Android wire. Provider rows are fixtures. */
class LivePriceE2ETest {
    private class TestClock(var at: Instant) : Clock() {
        override fun instant() = at
        override fun getZone(): ZoneId = ZoneId.of("Asia/Seoul")
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(at, zone)
    }
    @Test fun collectedTransactionsReachCallbackAndChangeWithoutWaitingForGeocoder() = runBlocking {
        val clock = TestClock(Instant.parse("2026-11-15T03:00:00Z"))
        val cache = InMemoryPreparedMarket(clock)
        val locations = InMemoryPriceLocation(clock)
        val voice = InMemoryVoiceContext(clock).apply { setConsented(true); setForeground(true); setAppState(true, null) }
        val requests = AtomicInteger()
        var amounts = listOf(111_000L, 222_000L, 333_000L)
        val geocoding = Channel<Unit>(Channel.UNLIMITED)
        val district = MarketDistrict("41117", "Fixture district")
        val source = object : ApartmentTradeRepository {
            override suspend fun getTrades(districtCode: String, contractMonth: YearMonth, pageNumber: Int, pageSize: Int): Outcome<TradePage> {
                requests.incrementAndGet()
                val trades = if (contractMonth == YearMonth.from(LocalDate.now(clock))) amounts.mapIndexed { i, amount ->
                    ApartmentTrade("$i", "Fixture $i", district.code, "fixture", "1", amount, BigDecimal(85), LocalDate.now(clock), 3, 2000, null, null, null)
                } else emptyList()
                return Outcome.Success(TradePage(trades, pageNumber, pageSize, trades.size))
            }
        }
        val geocoder = object : AddressGeocoder {
            override suspend fun geocode(address: String, kind: AddressKind): Outcome<GeoCoordinates> { geocoding.send(Unit); awaitCancellation() }
        }
        val interests = object : InterestRepository {
            override suspend fun get(): InterestCondition? = null
            override fun observe(): Flow<InterestCondition?> = flowOf(null)
            override suspend fun save(condition: InterestCondition) {}
            override suspend fun markSkipped() {}
            override suspend fun clear() {}
            override suspend fun isDecided() = true
        }
        val resolver = object : MarketDistrictResolver {
            override suspend fun districts(center: GeoPoint, radiusKm: Double) = Outcome.Success(listOf(district))
            override suspend fun districtAt(point: GeoPoint) = Outcome.Success(district)
        }
        val collector = LiveMarketRepository(source, geocoder, interests,
            object : VehicleLocationProvider { override val locations = flowOf(VehicleLocationState()) }, resolver, clock,
            preparedMarket = cache, traceSink = {}, priceLocations = locations)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val responses = LinkedBlockingQueue<Intent>()
        val receiver = object : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) { responses.offer(intent) } }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(BroadcastGleoCallbackSender.TEST_RESPONSE_ACTION), ContextCompat.RECEIVER_EXPORTED)
        val real = LiveAreaPriceSummaryRepository(cache, locations, voice, clock)
        val handler = GleoAndroidHandler(GleoIntentAdapter(GetAreaPriceSummaryUseCase(real),
            SaveInterestAreaUseCase(UnavailableInterestAreaRepository()), GetDailyBriefingUseCase(UnavailableDailyBriefingRepository()),
            BroadcastGleoCallbackSender(context), { AppCallContext("r", "s", "fixture", voice.snapshot(), locations.snapshot()) }))
        suspend fun query(target: String = "NAMED_AREA"): JSONObject {
            val params = JSONObject().put("target", target).apply { if (target == "NAMED_AREA") put("areaQuery", district.name) }
            val before = requests.get()
            handler.handle(Intent("GET_AREA_PRICE_SUMMARY").putExtra("ACTION_PARAM", params.toString()))
            val response = JSONObject(responses.poll(5, TimeUnit.SECONDS)!!.getStringExtra("response")!!)
            assertEquals(before, requests.get()) // Voice never starts a provider fetch.
            assertEquals("silent", response.getString("mode"))
            return response
        }
        fun data(response: JSONObject) = JSONArray(response.getString("data")).getJSONObject(0)
        var job: Job? = null
        try {
            assertEquals("asking", query().getString("status"))
            job = launch(Dispatchers.Default) { collector.refresh() }
            withTimeout(5_000) { geocoding.receive() }
            val started = System.nanoTime()
            val response = query()
            assertTrue("Price callback waited for collector", (System.nanoTime() - started) / 1_000_000 < 2_000)
            assertTrue(job.isActive)
            assertEquals("success", response.getString("status"))
            val first = data(response)
            assertEquals("LIVE", first.getString("executionMode")); assertFalse(first.getBoolean("synthetic"))
            assertEquals("molit-apartment-sale", first.getString("sourceId"))
            assertEquals(2_220_000_000L, first.getLong("averagePriceKrw"))
            assertEquals(3_330_000_000L, first.getLong("highestPriceKrw")); assertEquals("Fixture 2", first.getString("highestComplexName"))
            assertEquals("DISTRICT", first.getString("scope")); assertEquals("AVAILABLE", first.getString("sampleStatus"))
            assertEquals("2026-08-15", first.getString("periodStart")); assertEquals("2026-11-16", first.getString("periodEndExclusive"))
            job.cancelAndJoin()
            clock.at = clock.at.plusSeconds(301)
            assertEquals("STALE_DATA", data(query()).getString("code"))
            amounts = listOf(450_000L)
            job = launch(Dispatchers.Default) { collector.refresh() }
            withTimeout(5_000) { geocoding.receive() }
            val changed = data(query())
            assertEquals(4_500_000_000L, changed.getLong("averagePriceKrw")); assertEquals("INSUFFICIENT", changed.getString("sampleStatus"))
            job.cancelAndJoin()
            clock.at = clock.at.plusSeconds(301); amounts = emptyList()
            job = launch(Dispatchers.Default) { collector.refresh() }
            withTimeout(5_000) { job.join() }
            val empty = data(query())
            assertEquals("EMPTY", empty.getString("sampleStatus")); assertTrue(empty.isNull("averagePriceKrw")); assertTrue(empty.isNull("highestPriceKrw"))
            assertEquals("asking", query("CURRENT_LOCATION").getString("status"))
        } finally { job?.cancelAndJoin(); context.unregisterReceiver(receiver) }
    }
}
