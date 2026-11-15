package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.data.repository.*
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.port.*
import java.math.BigDecimal
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class PreparedMarketConcurrencyTest {
    @Test fun voiceReadDoesNotWaitForCollectorFetchOrGeocodeMutex() = runBlocking {
        val clock=Clock.fixed(Instant.parse("2026-11-15T03:00:00Z"),ZoneId.of("Asia/Seoul"))
        val cache=InMemoryPreparedMarket(clock)
        val seed=PreparedScrapCandidate("previous","previous","dong",25,PriceRecord(100_000,LocalDate.of(2026,11,14)),null,clock.instant(),"11111")
        cache.publish(listOf(seed))
        val fetchStarted=CompletableDeferred<Unit>();val fetchGate=CompletableDeferred<Unit>();val geocodeStarted=CompletableDeferred<Unit>()
        val trade=ApartmentTrade("a","Synthetic A","41117","dong","1",100_000,BigDecimal(85),LocalDate.of(2026,11,14),3,2001,null,null,null)
        val source=object:ApartmentTradeRepository {
            override suspend fun getTrades(districtCode: String,contractMonth: YearMonth,pageNumber: Int,pageSize: Int): Outcome<TradePage> {
                fetchStarted.complete(Unit);fetchGate.await()
                val rows=if(contractMonth==YearMonth.of(2026,11)) listOf(trade) else emptyList()
                return Outcome.Success(TradePage(rows,pageNumber,pageSize,rows.size))
            }
        }
        val geocoder=object:AddressGeocoder {
            override suspend fun geocode(address: String,kind: AddressKind): Outcome<GeoCoordinates> {
                geocodeStarted.complete(Unit);awaitCancellation()
            }
        }
        val interests=object:InterestRepository {
            override suspend fun get(): InterestCondition?=null
            override fun observe(): Flow<InterestCondition?> = flowOf(null)
            override suspend fun save(condition: InterestCondition) {}
            override suspend fun markSkipped() {}
            override suspend fun clear() {}
            override suspend fun isDecided()=true
        }
        val location=object:VehicleLocationProvider { override val locations=flowOf(VehicleLocationState()) }
        val district=MarketDistrict("41117","Synthetic district")
        val resolver=object:MarketDistrictResolver {
            override suspend fun districts(center: GeoPoint,radiusKm: Double)=Outcome.Success(listOf(district))
            override suspend fun districtAt(point: GeoPoint)=Outcome.Success(district)
        }
        val repo=LiveMarketRepository(source,geocoder,interests,location,resolver,clock,preparedMarket=cache,traceSink={})
        val job=launch(Dispatchers.Default) { repo.refresh() }
        try {
            withTimeout(5_000) { fetchStarted.await() }
            assertEquals(Outcome.Success(seed),cache.readScrapCandidate("previous"))
            assertTrue(cache.readScrapCandidate("molit:41117:a") is Outcome.Failure)
            fetchGate.complete(Unit)
            withTimeout(5_000) { geocodeStarted.await() }
            assertTrue(job.isActive) // Collector still holds its network/geocoding mutex.
            val started=System.nanoTime()
            repeat(1_000) { assertTrue(cache.readScrapCandidate("molit:41117:a") is Outcome.Success) }
            val millis=(System.nanoTime()-started)/1_000_000
            println("PreparedMarketPerf: 1000 cache reads: ${millis}ms while collector blocked")
            assertTrue("1000 memory reads took ${millis}ms",millis<1_000)
            assertEquals(1,(cache.readDistrictPrices("41117") as Outcome.Success).value.sampleCount)
        } finally { job.cancelAndJoin() }
    }
}
