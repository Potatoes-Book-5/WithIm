package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.port.*
import java.math.BigDecimal
import java.time.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LiveAreaPriceSummaryRepositoryTest {
    private val start = Instant.parse("2026-11-15T03:00:00Z")
    private class MutableClock(var at: Instant) : Clock() {
        override fun instant() = at
        override fun getZone(): ZoneId = ZoneId.of("Asia/Seoul")
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(at, zone)
    }
    private val clock = MutableClock(start)
    private val voices = InMemoryVoiceContext(clock).apply {
        setConsented(true); setForeground(true); setAppState(true, null)
    }
    private val locations = InMemoryPriceLocation(clock)
    private val prices = InMemoryPreparedMarket(clock)
    private val repo = LiveAreaPriceSummaryRepository(prices, locations, voices, clock)
    private val current = AreaTarget(AreaTargetKind.CURRENT_LOCATION)
    private val named = AreaTarget(AreaTargetKind.NAMED_AREA, "영통구")
    private val previous = AreaTarget(AreaTargetKind.LAST_REFERENCED_AREA)
    private fun context(session: String = "s", profile: String = "p") =
        AppCallContext("r", session, profile, voices.snapshot(), locations.snapshot())
    private fun seed(code: String = "41117", name: String = "경기도 수원시 영통구", amounts: List<Long> = listOf(210_000, 310_000, 230_000)) {
        val to = LocalDate.now(clock)
        val trades = amounts.mapIndexed { index, amount ->
            val id = "molit:$code:$index"
            id to ApartmentTrade(id, "Fixture $index", code, "fixture", "1", amount, BigDecimal(85), to, 3, 2001, null, null, null)
        }
        val batch = prepareMarketBatch(code, name, to.minusMonths(3), to, clock.instant(), trades)
        prices.publish(batch.candidates, batch.prices)
    }
    private fun fix(accuracy: Double? = 10.0, at: Instant = clock.instant(), point: GeoPoint = GeoPoint(37.25, 127.05),
                    district: MarketDistrict = MarketDistrict("41117", "경기도 수원시 영통구")): VehicleLocationState {
        val fix = VehicleLocationState(VehiclePose(point), accuracy, at, LocationStatus.AVAILABLE, LocationSource.EMULATOR_ROUTE)
        locations.updateFix(fix)
        locations.publish(fix, district, locations.revision())
        return fix
    }
    private fun payload(outcome: Outcome<ActionResult<PriceSummaryPayload>>) =
        ((outcome as Outcome.Success).value as ActionResult.Completed).payload
    private fun assertAsking(outcome: Outcome<ActionResult<PriceSummaryPayload>>) =
        assertEquals(Outcome.Success(ActionResult.NeedsInput("areaQuery")), outcome)

    @Test fun currentAndNamedReadActualRawMeanAndHighestNameWithoutDummyValues() = runTest {
        seed(); fix()
        val result = payload(repo.get(current, context()))
        assertEquals(2_500_000_000L, result.averagePriceKrw)
        assertEquals(3_100_000_000L, result.highestPriceKrw)
        assertEquals("Fixture 1", result.highestComplexName)
        assertEquals("molit:41117:1", result.highestComplexId)
        assertEquals(3, result.sampleCount)
        assertEquals("DISTRICT", result.scope)
        assertEquals("2026-08-15", result.periodStart)
        assertEquals("2026-11-16", result.periodEndExclusive)
        assertEquals(ExecutionMode.LIVE, result.metadata.executionMode)
        assertFalse(result.metadata.synthetic)
        assertEquals("molit-apartment-sale", result.metadata.sourceId)
        assertEquals(start.toString(), result.locationObservedAt)
        assertEquals("EMULATOR_ROUTE", result.locationSource)
        assertNull(payload(repo.get(named, context())).locationObservedAt)
    }
    @Test fun replacementChangesTheActualResult() = runTest {
        seed(); assertEquals(2_500_000_000L, payload(repo.get(named, context())).averagePriceKrw)
        clock.at = start.plusSeconds(1)
        seed(amounts = listOf(440_000))
        val result = payload(repo.get(named, context()))
        assertEquals(4_400_000_000L, result.averagePriceKrw)
        assertEquals(1, result.sampleCount)
    }
    @Test fun emptyCompleteBatchReturnsNullPricesRatherThanZeroOrFiction() = runTest {
        seed(amounts = emptyList())
        val result = payload(repo.get(named, context()))
        assertEquals(0, result.sampleCount)
        assertNull(result.averagePriceKrw); assertNull(result.highestPriceKrw)
        assertNull(result.highestComplexId); assertNull(result.highestComplexName)
    }
    @Test fun missingIdAndNameNeverBecomeAnInventedComplex() = runTest {
        val to = LocalDate.now(clock)
        val trade = ApartmentTrade(null, null, "41117", "n", "1", 210_000, BigDecimal(85), to, 1, 2000, null, null, null)
        val batch = prepareMarketBatch("41117", "경기도 수원시 영통구", to.minusMonths(3), to, start, listOf(null to trade))
        prices.publish(batch.candidates, batch.prices)
        val result = payload(repo.get(named, context()))
        assertEquals(1, result.sampleCount); assertNull(result.highestComplexId); assertNull(result.highestComplexName)
    }
    @Test fun coldAndFiveMinuteOldDataFailImmediately() = runTest {
        fix()
        assertEquals(Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.NOT_READY)), repo.get(current, context()))
        seed(); clock.at = start.plusSeconds(300)
        assertEquals(Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.STALE)), repo.get(named, context()))
    }
    @Test fun missingUnreportedInaccurateOldAndFutureLocationAskButExplicitAreaWorks() = runTest {
        seed(); assertAsking(repo.get(current, context()))
        for ((accuracy, at) in listOf(null to start, 50.01 to start, 10.0 to start.minusSeconds(11), 10.0 to start.plusSeconds(1))) {
            locations.updateFix(VehicleLocationState())
            fix(accuracy, at)
            assertAsking(repo.get(current, context()))
        }
        locations.updateFix(VehicleLocationState())
        fix(50.0, start.minusSeconds(10))
        assertEquals(3, payload(repo.get(current, context())).sampleCount)
        locations.updateFix(VehicleLocationState(status = LocationStatus.UNSUPPORTED))
        assertEquals(3, payload(repo.get(named, context())).sampleCount)
    }
    @Test fun ambiguousNamesAndUnknownNeighborhoodsAskWithoutInventingAnArea() = runTest {
        seed("11140", "서울특별시 중구"); seed("26110", "부산광역시 중구")
        assertAsking(repo.get(AreaTarget(AreaTargetKind.NAMED_AREA, "중구"), context()))
        assertAsking(repo.get(AreaTarget(AreaTargetKind.NAMED_AREA, "성수동2가"), context()))
        assertEquals("11140", payload(repo.get(AreaTarget(AreaTargetKind.NAMED_AREA, "서울특별시 중구"), context())).area.id)
    }
    @Test fun referenceIsScopedAndDoesNotExtendItsOwnSixtySecondTtl() = runTest {
        seed(); payload(repo.get(named, context()))
        assertAsking(repo.get(previous, context(session = "other")))
        assertAsking(repo.get(previous, context(profile = "other")))
        clock.at = start.plusSeconds(60)
        assertEquals("41117", payload(repo.get(previous, context())).area.id)
        clock.at = start.plusSeconds(60).plusMillis(1)
        assertAsking(repo.get(previous, context()))
    }
    @Test fun currentLocationNeverFallsBackToSuccessfulPreviousArea() = runTest {
        seed(); payload(repo.get(named, context()))
        assertAsking(repo.get(current, context()))
        voices.setConsented(false); voices.setConsented(true)
        assertAsking(repo.get(previous, context()))
    }
    @Test fun acceptedLocationIsFrozenButPermissionChangeInvalidatesIt() = runTest {
        seed(); fix(); val accepted = context()
        fix(point = GeoPoint(37.5, 127.5), district = MarketDistrict("11140", "서울특별시 중구"))
        assertEquals("41117", payload(repo.get(current, accepted)).area.id)
        locations.updateFix(VehicleLocationState(status = LocationStatus.PERMISSION_REQUIRED))
        assertAsking(repo.get(current, accepted))
    }
    @Test fun consentRevokedDuringReadPreventsResultAndReferencePublication() = runTest {
        seed()
        val reader = object : PreparedPriceReader by prices {
            override fun readDistrictPrices(districtCode: String): Outcome<PreparedDistrictPrices> {
                val result = prices.readDistrictPrices(districtCode)
                voices.setConsented(false)
                return result
            }
        }
        val guarded = LiveAreaPriceSummaryRepository(reader, locations, voices, clock)
        assertTrue(guarded.get(named, context()) is Outcome.Failure)
        voices.setConsented(true)
        assertAsking(guarded.get(previous, context()))
    }
    @Test fun malformedReadModelCannotEscapeAsRealPrices() = runTest {
        seed()
        val bad = (prices.readDistrictPrices("41117") as Outcome.Success).value.copy(averagePriceKrw = -1)
        val reader = object : PreparedPriceReader by prices {
            override fun readDistrictPrices(districtCode: String) = Outcome.Success(bad)
        }
        assertEquals(Outcome.Failure(DomainError.InvalidResponse), LiveAreaPriceSummaryRepository(reader, locations, voices, clock).get(named, context()))
    }
    @Test fun unsupportedFeaturesDoNotReturnInventedAnnouncementsOrPersistAnything() = runTest {
        assertTrue(UnavailableDailyBriefingRepository().get(context()) is Outcome.Failure)
        assertTrue(UnavailableInterestAreaRepository().save(current, context()) is Outcome.Failure)
    }
}
