package com.potatoes.book5.realestate.data.remote

import com.potatoes.book5.realestate.domain.error.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TourNoticeSourcesTest {
    private val rss="""<rss version="2.0"><channel><item><title>주택 공급 정책</title><link>https://www.molit.go.kr/fixture</link><pubDate>Sun, 15 Nov 2026 03:00:00 GMT</pubDate><description>Article body must not be copied.</description></item></channel></rss>"""
    private fun page(title:String="Fixture apartment",total:Int=1)="""{"matchCount":$total,"data":[{"HOUSE_MANAGE_NO":"202600001","PBLANC_NO":"202600002","HOUSE_NM":"$title","HSSPLY_ADRES":"경기도 수원시 영통구 광교로 1","RCRIT_PBLANC_DE":"2026-11-15"}]}"""
    @Test fun rssKeepsOfficialTitleLinkAndActualPublicationTimeWithoutBody() {
        val row=TourNoticeParsers.rss(rss).single()
        assertEquals("NATIONAL_PRESS",row.type);assertEquals("NATIONAL",row.scope)
        assertEquals("2026-11-15T03:00:00Z",row.publishedAt.toString())
        assertFalse(row.toString().contains("Article body"))
        assertEquals("https://www.molit.go.kr/fixture",row.url)
    }
    @Test fun missingRssDateIsUnknownAndHttpOfficialLinksUpgradeToHttps() {
        val row=TourNoticeParsers.rss(rss.replace("<pubDate>Sun, 15 Nov 2026 03:00:00 GMT</pubDate>","").replace("https://","http://")).single()
        assertNull(row.publishedAt);assertTrue(row.url.startsWith("https://"))
    }
    @Test fun externalEntitiesOversizeAndUnofficialLinksAreRejected() {
        for(xml in listOf("<!DOCTYPE rss [<!ENTITY ex SYSTEM 'file:///etc/passwd'>]>$rss",rss.replace("www.molit.go.kr","evil.invalid"),"x".repeat(500001))) {
            assertTrue(runCatching { TourNoticeParsers.rss(xml) }.isFailure)
        }
    }
    @Test fun rssFailureIsUnavailableRatherThanAnEmptySuccessfulFeed() = runTest {
        val source=MolitRssNoticeSource { Outcome.Failure(DomainError.Http(403)) }
        assertEquals(Outcome.Failure(DomainError.Http(403)),source.fetch())
    }
    @Test fun applicationAnnouncementPreservesDatePrecisionAndOfficialIds() {
        val result=TourNoticeParsers.applyhome(page());val row=result.rows.single()
        assertEquals(1,result.total);assertEquals("notice:applyhome:202600001:202600002",row.id)
        assertNull(row.publishedAt);assertEquals("2026-11-15",row.publishedDate.toString())
        assertEquals("DISTRICT_ADDRESS_MATCH",row.scope);assertEquals("SUBSCRIPTION",row.source)
    }
    @Test fun unconfiguredApplyhomeNeverUsesAnotherServicesKeyOrCallsTransport() = runTest {
        var requests=0
        val source=ApplyhomeNoticeSource(fetchPage={ _,_,_ -> requests++;Outcome.Success(page()) })
        assertFalse(source.configured)
        assertEquals(Outcome.Failure(DomainError.MissingApiKey),source.fetch());assertEquals(0,requests)
    }
    @Test fun configuredApplyhomeUsesDocumentedPaginationAndRejectsRepeatedPages() = runTest {
        var requests=0
        val source=ApplyhomeNoticeSource("test-only",fetchPage={ _,_,key -> assertEquals("test-only",key);requests++;Outcome.Success(page()) })
        assertTrue(source.fetch() is Outcome.Success);assertEquals(1,requests)
        val duplicate=ApplyhomeNoticeSource("test-only",fetchPage={ _,_,_ -> Outcome.Success(page(total=2)) })
        assertEquals(Outcome.Failure(DomainError.InvalidResponse),duplicate.fetch())
    }
    @Test fun malformedAnnouncementsCannotBecomeNews() {
        assertTrue(runCatching { TourNoticeParsers.applyhome(page().replace("2026-11-15","not-a-date")) }.isFailure)
        assertTrue(runCatching { TourNoticeParsers.applyhome(page(total=501)) }.isFailure)
    }
}
