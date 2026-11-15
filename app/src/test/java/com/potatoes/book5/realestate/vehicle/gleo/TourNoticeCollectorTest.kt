package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.data.remote.*
import com.potatoes.book5.realestate.data.repository.*
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TourNoticeCollectorTest {
    private val feed="""<rss><channel><item><title>주택 공급 정책</title><link>https://www.molit.go.kr/fixture</link></item></channel></rss>"""
    @Test fun actualRssParserCollectorAndBriefingConnectWithoutVoiceFetchingOrFakeAnnouncements() = runTest {
        val f=TourFixture();f.prepare(this);f.saved();f.seedBriefing()
        var requests=0
        val sources=listOf(MolitRssNoticeSource { requests++;Outcome.Success(feed) },ApplyhomeNoticeSource())
        val collector=TourNoticeCollector(sources,f.store,f.market,f.access,f.voices,f::context,f.clock)
        assertTrue(collector.refresh(f.context()) is Outcome.Success)
        val repository=LiveDailyBriefingRepository(f.store,f.market,f.access,sources,f.clock)
        val payload=((repository.get(f.context()) as Outcome.Success).value as ActionResult.Completed).payload
        assertEquals(setOf("PRICE_SUMMARY_UPDATE","NATIONAL_PRESS"),payload.items.map { it.type }.toSet())
        assertEquals("READY",payload.sourceStates["NEWS"]);assertEquals("UNCONFIGURED",payload.sourceStates["SUBSCRIPTION"])
        assertNull(payload.items.find { it.type=="NATIONAL_PRESS" }!!.publishedAt)
        repository.get(f.context());assertEquals(1,requests)
        assertTrue(f.store.briefings.all { it.readVersion==null })
    }
    @Test fun repeatPreparationDoesNotRefetchOrRenewCollectionTimestamp() = runTest {
        val f=TourFixture();f.prepare(this);f.saved()
        var requests=0
        val sources=listOf(MolitRssNoticeSource { requests++;Outcome.Success(feed) })
        val collector=TourNoticeCollector(sources,f.store,f.market,f.access,f.voices,f::context,f.clock)
        collector.refresh(f.context());val first=f.store.statuses["p" to "NEWS"]!!.attemptedAt
        f.clock.at=f.clock.at.plusSeconds(1)
        collector.refresh(f.context())
        assertEquals(1,requests);assertEquals(first,f.store.statuses["p" to "NEWS"]!!.attemptedAt)
    }
    @Test fun revokedContextDiscardsSourceResponseAndDrivingDoesNotPoll() = runTest {
        val f=TourFixture();f.prepare(this);f.saved()
        var requests=0
        val source=MolitRssNoticeSource { requests++;f.voices.setConsented(false);Outcome.Success(feed) }
        val collector=TourNoticeCollector(listOf(source),f.store,f.market,f.access,f.voices,f::context,f.clock)
        assertTrue(collector.refresh(f.context()) is Outcome.Failure)
        assertTrue(f.store.notices.isEmpty());assertTrue(f.store.statuses.isEmpty())
        f.voices.setConsented(true);f.voices.setAppState(true,"a",visualDetailsAllowed=false)
        assertTrue(collector.refresh(f.context()) is Outcome.Failure);assertEquals(1,requests)
    }
    @Test fun savedDistrictIdentitySurvivesEmptyPreparedCatalogAndMissingIdentityReportsPartial() = runTest {
        val f=TourFixture();f.prepare(this);f.saved()
        val emptyMarket=InMemoryPreparedMarket(f.clock)
        val source=object:com.potatoes.book5.realestate.domain.port.TourNoticeSource {
            override val id="SUBSCRIPTION"
            override val configured=true
            override suspend fun fetch()=Outcome.Success(listOf(com.potatoes.book5.realestate.domain.model.tour.TourNotice(
                "notice:applyhome:fixture","SUBSCRIPTION","SUBSCRIPTION","공급 공고","https://www.data.go.kr/fixture",null,"경기도 수원시 영통구 광교로","DISTRICT")))
        }
        val collector=TourNoticeCollector(listOf(source),f.store,emptyMarket,f.access,f.voices,f::context,f.clock)
        assertTrue(collector.refresh(f.context()) is Outcome.Success)
        assertEquals(1,f.store.notices.size)
        assertEquals("READY",f.store.statuses["p" to "SUBSCRIPTION"]!!.state)
        f.store.rows.value=f.store.rows.value.map { it.copy(area=it.area.copy(districtName=null)) }
        collector.refresh(f.context())
        assertEquals("PARTIAL_AREA_MATCH",f.store.statuses["p" to "SUBSCRIPTION"]!!.state)
    }
}
