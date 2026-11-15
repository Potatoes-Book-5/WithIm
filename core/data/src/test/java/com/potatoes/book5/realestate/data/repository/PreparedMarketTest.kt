package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import java.math.BigDecimal
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class PreparedMarketTest {
    private val instant = Instant.parse("2026-11-15T03:00:00Z")
    private class TestClock(var at: Instant) : Clock() {
        override fun instant() = at
        override fun getZone(): ZoneId = ZoneId.of("Asia/Seoul")
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(at, zone)
    }
    private fun candidate(id: String, at: Instant = instant) = PreparedScrapCandidate(id,"Synthetic $id","dong",25,
        PriceRecord(100_000,LocalDate.of(2026,11,14)),null,at,"41117")
    private val start = LocalDate.of(2026,8,15)
    private val end = LocalDate.of(2026,11,15)
    private fun trade(id: String, amount: Long, date: LocalDate=end, area: String="85") = id to ApartmentTrade(
        id,"Synthetic $id","41117","dong","1",amount,BigDecimal(area),date,3,2001,null,null,null)
    @Test fun coldExpiryAndFutureClockAreExplicit() {
        val clock=TestClock(instant); val cache=InMemoryPreparedMarket(clock)
        assertEquals(Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.NOT_READY)),cache.readScrapCandidate("a"))
        cache.publish(listOf(candidate("a")))
        assertTrue(cache.readScrapCandidate("a") is Outcome.Success)
        clock.at=instant.plusSeconds(300)
        assertEquals(Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.STALE)),cache.readScrapCandidate("a"))
        cache.publish(listOf(candidate("a"))) // Rerender does not renew freshness.
        assertTrue(cache.readScrapCandidate("a") is Outcome.Failure)
        clock.at=instant.minusSeconds(1)
        assertTrue(cache.readScrapCandidate("a") is Outcome.Failure)
    }
    @Test fun inputIsCopiedAndBoundedEvictionPrefersRepublishedTargets() {
        val cache=InMemoryPreparedMarket(TestClock(instant), maxCandidates=2)
        val rows=mutableListOf(candidate("a"),candidate("b"));cache.publish(rows);rows.clear()
        assertTrue(cache.readScrapCandidate("a") is Outcome.Success)
        cache.publish(listOf(candidate("a"),candidate("c")))
        assertTrue(cache.readScrapCandidate("a") is Outcome.Success)
        assertTrue(cache.readScrapCandidate("b") is Outcome.Failure)
        cache.publish(listOf(candidate("a",instant.minusSeconds(10))))
        assertEquals(instant,(cache.readScrapCandidate("a") as Outcome.Success).value.sourceAsOf)
    }
    @Test fun rawAverageIsNotAnAverageOfApartmentMaximumsAndBaselineIsLatest() {
        val batch=prepareMarketBatch("41117","Synthetic district",start,end,instant,listOf(
            trade("a",200_000,start),trade("a",100_000,end),trade("b",300_000,end)))
        assertEquals(3,batch.prices.sampleCount)
        assertEquals(2_000_000_000L,batch.prices.averagePriceKrw)
        assertEquals(3_000_000_000L,batch.prices.highestPriceKrw)
        assertEquals(100_000L,batch.candidates.single { it.apartmentId=="a" }.latest.priceManwon)
        assertEquals("DISTRICT",batch.prices.scope)
    }
    @Test fun coverageAreaCancellationAndPriceFiltersAreApplied() {
        val cancelled=trade("cancel",999_000).let { it.first to it.second.copy(cancellationType="notice") }
        val wrong=trade("other",999_000).let { it.first to it.second.copy(districtCode="11111") }
        val rows=listOf(trade("start",100_000,start,"80"),trade("end",300_000,end,"89.99"),
            trade("small",999_000,area="79.99"),trade("large",999_000,area="90"),
            trade("future",999_000,end.plusDays(1)),trade("old",999_000,start.minusDays(1)),
            trade("bad",-1),trade("overflow",Long.MAX_VALUE),cancelled,wrong)
        val summary=prepareMarketBatch("41117","district",start,end,instant,rows).prices
        assertEquals(2,summary.sampleCount);assertEquals(2_000_000_000L,summary.averagePriceKrw)
    }
    @Test fun emptyCompleteSummaryIsDifferentFromMissingAndDistrictBoundIsApplied() {
        val cache=InMemoryPreparedMarket(TestClock(instant),maxDistricts=1)
        val empty=prepareMarketBatch("a","a",start,end,instant,emptyList()).prices
        cache.publish(emptyList(),empty)
        val result=(cache.readDistrictPrices("a") as Outcome.Success).value
        assertEquals(0,result.sampleCount);assertNull(result.averagePriceKrw)
        cache.publish(emptyList(),empty.copy(districtCode="b"))
        assertTrue(cache.readDistrictPrices("a") is Outcome.Failure)
    }
    @Test fun averagingUsesWideAccumulator() {
        val max=Long.MAX_VALUE/10_000
        val result=prepareMarketBatch("41117","d",start,end,instant,listOf(trade("a",max),trade("b",max)))
        assertEquals(max*10_000,result.prices.averagePriceKrw)
    }
    @Test fun fullReplacementRemovesCancelledTargetsAndRejectsOlderPartialUpdates() {
        val clock=TestClock(instant);val cache=InMemoryPreparedMarket(clock)
        val empty=prepareMarketBatch("41117","d",start,end,instant,emptyList()).prices
        cache.publish(listOf(candidate("a")),empty)
        clock.at=instant.plusSeconds(10)
        cache.publish(emptyList(),empty.copy(sourceAsOf=clock.at))
        assertTrue(cache.readScrapCandidate("a") is Outcome.Failure)
        cache.publish(listOf(candidate("a")))
        assertTrue(cache.readScrapCandidate("a") is Outcome.Failure)
    }
    @Test fun correctedClockCanReplaceFutureInvalidData() {
        val clock=TestClock(instant);val cache=InMemoryPreparedMarket(clock)
        cache.publish(listOf(candidate("a")))
        clock.at=instant.minusSeconds(30)
        cache.publish(listOf(candidate("a",clock.at)))
        assertTrue(cache.readScrapCandidate("a") is Outcome.Success)
    }
    @Test fun unidentifiedTransactionsStillContributeToDistrictMean() {
        val unknown=trade("unknown",300_000).second.copy(apartmentId=null,apartmentName=null,lotNumber=null)
        val result=prepareMarketBatch("41117","d",start,end,instant,listOf(trade("a",100_000),null to unknown))
        assertEquals(2,result.prices.sampleCount);assertEquals(2_000_000_000L,result.prices.averagePriceKrw)
        assertNull(result.prices.highestComplexId)
        assertEquals(1,result.candidates.size)
    }
    @Test fun priorDaySummaryCannotBeServedAsTodayEvenWithinTtl() {
        val clock=TestClock(Instant.parse("2026-11-15T14:59:59Z"))
        val cache=InMemoryPreparedMarket(clock)
        val summary=prepareMarketBatch("41117","d",start,end,clock.at,emptyList()).prices
        cache.publish(emptyList(),summary)
        clock.at=clock.at.plusSeconds(2)
        assertEquals(Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.STALE)),cache.readDistrictPrices("41117"))
    }
    @Test fun equalTimestampPartialRequiresCurrentBatchRevisionToRestoreEvictedVisibleTargets() {
        val cache=InMemoryPreparedMarket(TestClock(instant),maxCandidates=1)
        val summary=prepareMarketBatch("41117","d",start,end,instant,emptyList()).prices
        val token=cache.publish(listOf(candidate("a"),candidate("b")),summary)
        cache.publish(listOf(candidate("a")))
        assertTrue(cache.readScrapCandidate("a") is Outcome.Failure)
        cache.publish(listOf(candidate("a")),expectedRevision=token)
        assertTrue(cache.readScrapCandidate("a") is Outcome.Success)
        cache.publish(emptyList(),summary)
        cache.publish(listOf(candidate("a")),expectedRevision=token)
        assertTrue(cache.readScrapCandidate("a") is Outcome.Failure)
    }
    @Test fun malformedAndFutureDatedDetailCandidatesAreNotPublished() {
        val cache=InMemoryPreparedMarket(TestClock(instant))
        val normal=candidate("a")
        cache.publish(listOf(normal.copy(latest=normal.latest.copy(priceManwon=-1)),
            candidate("b").copy(areaPyeong=0), candidate("c").copy(latest=normal.latest.copy(dealDate=end.plusDays(1)))))
        listOf("a","b","c").forEach { assertTrue(cache.readScrapCandidate(it) is Outcome.Failure) }
    }
}
