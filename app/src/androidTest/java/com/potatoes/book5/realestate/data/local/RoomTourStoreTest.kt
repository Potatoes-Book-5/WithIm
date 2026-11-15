package com.potatoes.book5.realestate.data.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.potatoes.book5.realestate.data.local.db.AppDatabase
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.tour.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RoomTourStoreTest {
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    private val at=Instant.parse("2026-11-15T03:00:00Z")
    private val area=TourArea("poi:fixture","Fixture station","41117",AreaBoundary.Circle(GeoPoint(37.25,127.05)),"fixture-provider",at,"경기도 수원시 영통구")
    private val prices=PreparedDistrictPrices("41117","Fixture district",LocalDate.of(2026,8,15),LocalDate.of(2026,11,15),3,
        1_000_000_000L,2_000_000_000L,"a",at,highestComplexName="Fixture A")
    private suspend fun <T> withMemory(block:suspend (RoomTourStore)->T): T {
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try { return block(RoomTourStore(db)) } finally { db.close() }
    }
    private fun <T> value(result:Outcome<T>)=(result as Outcome.Success).value

    @Test fun savedAreaAndBriefingSurviveDatabaseReopenWithoutDuplicateRows() = runBlocking {
        val name="tour-test-${UUID.randomUUID()}.db"
        var db=Room.databaseBuilder(context,AppDatabase::class.java,name).build()
        try {
            var store=RoomTourStore(db)
            val saved=value(store.ensureArea("p",area,at) { true })
            assertFalse(saved.alreadySaved)
            assertTrue(value(store.ensureArea("p",area,at) { true }).alreadySaved)
            assertTrue(value(store.publishSummary("p",area.id,prices,at)))
            db.close();db=Room.databaseBuilder(context,AppDatabase::class.java,name).build();store=RoomTourStore(db)
            assertEquals(saved.saved,value(store.areas("p")).single())
            assertEquals(1,value(store.unread("p")).size)
            assertTrue(value(store.areas("other-profile")).isEmpty())
        } finally { db.close();context.deleteDatabase(name) }
    }
    @Test fun summaryVersionAndAcknowledgementAreIdempotentAndRejectOldReceipts() = runBlocking { withMemory { store ->
        store.ensureArea("p",area,at) { true }
        assertTrue(value(store.publishSummary("p",area.id,prices,at)))
        val first=value(store.unread("p")).single()
        assertFalse(value(store.publishSummary("p",area.id,prices.copy(sourceAsOf=at.plusSeconds(1)),at.plusSeconds(1))))
        assertEquals(first.observedAt,value(store.unread("p")).single().observedAt)
        assertEquals(1,value(store.acknowledge("p",listOf(BriefingReceipt("price-summary:${area.id}",first.version)))))
        assertTrue(value(store.unread("p")).isEmpty())
        assertTrue(value(store.publishSummary("p",area.id,prices.copy(averagePriceKrw=1_500_000_000L,sourceAsOf=at.plusSeconds(2)),at.plusSeconds(2))))
        assertEquals(0,value(store.acknowledge("p",listOf(BriefingReceipt(area.id,first.version)))))
        assertEquals(1_500_000_000L,value(store.unread("p")).single().averagePriceKrw)
        assertFalse(value(store.publishSummary("p",area.id,prices,at)))
    } }
    @Test fun announcementVersionsProfileIsolationAndCascadesUseRealSqlite() = runBlocking { withMemory { store ->
        store.ensureArea("p",area,at) { true };store.ensureArea("p",area.copy(id="second"),at) { true }
        store.ensureArea("q",area,at) { true }
        val notice=TourNotice("notice:news:1","NEWS","NATIONAL_PRESS","주택 공급 정책","https://www.molit.go.kr/fixture",at)
        store.publishNotice("p",area.id,notice,at) { true };store.publishNotice("p","second",notice,at) { true }
        val first=value(store.unreadNotices("p")).single()
        assertEquals(1,value(store.acknowledge("p",listOf(BriefingReceipt(notice.id,first.version)))))
        assertTrue(value(store.unreadNotices("p")).isEmpty())
        store.publishNotice("q",area.id,notice,at) { true };assertEquals(1,value(store.unreadNotices("q")).size)
        store.publishNotice("p",area.id,notice.copy(title="주택 공급 정책 정정"),at.plusSeconds(2)) { true }
        assertEquals(0,value(store.acknowledge("p",listOf(BriefingReceipt(notice.id,first.version)))))
        assertFalse(value(store.publishNotice("p",area.id,notice,at) { true }))
        assertEquals("주택 공급 정책 정정",value(store.unreadNotices("p")).single().notice.title)
        store.setSourceState("p",TourSourceState("NEWS","READY",at)) { true }
        store.deleteArea("p",area.id)
        assertTrue(value(store.unreadNotices("p")).isEmpty())
        store.deleteAll();assertTrue(value(store.areas("q")).isEmpty());assertTrue(value(store.sourceStates("p")).isEmpty())
    } }
    @Test fun invalidatedTransactionsRollbackAndOldAreaGenerationsCannotRecordEntries() = runBlocking { withMemory { store ->
        var guards=0
        assertTrue(store.ensureArea("p",area,at) { ++guards==1 } is Outcome.Failure)
        assertTrue(value(store.areas("p")).isEmpty())
        val first=value(store.ensureArea("p",area,at) { true }).saved
        val entry=AreaEntry("p",area.id,"s",1,at,1,first.generation)
        assertTrue(store.recordEntry(entry) { true } is Outcome.Success)
        store.publishSummary("p",area.id,prices,at)
        val receipt=value(store.unread("p")).single()
        guards=0
        assertTrue(store.acknowledge("p",listOf(BriefingReceipt(area.id,receipt.version))) { ++guards==1 } is Outcome.Failure)
        assertEquals(1,value(store.unread("p")).size)
        store.deleteArea("p",area.id)
        assertTrue(store.recordEntry(entry) { true } is Outcome.Failure)
        val next=value(store.ensureArea("p",area,at) { true }).saved
        assertNotEquals(first.generation,next.generation)
        assertTrue(store.recordEntry(entry) { true } is Outcome.Failure)
        assertNull(value(store.latestEntry("p",area.id)))
        assertTrue(store.setSourceState("p",TourSourceState("NEWS","READY",at)) { false } is Outcome.Failure)
        assertTrue(value(store.sourceStates("p")).isEmpty())
    } }
}
