package com.potatoes.book5.realestate.data.repository
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.port.*
import com.potatoes.book5.realestate.domain.usecase.*
import java.time.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
class VoiceContextTest {
 private val time=Instant.parse("2026-11-15T00:00:00Z")
 private val store=InMemoryVoiceContext(Clock.fixed(time,ZoneOffset.UTC))
 private fun active() { store.setConsented(true);store.setForeground(true);store.setAppState(true,null,true) }
 private fun call()=AppCallContext("r","s","local",store.snapshot())
 private val detail=ApartmentDetail("a","Synthetic A","d","n",null,null,null,null,null,
  Deal(LocalDate.of(2026,11,14),30,1,10000),null,emptyList(),emptyList())
 private class Scraps : ScrapRepository {
  val rows=MutableStateFlow<List<Scrap>>(emptyList()); var writes=0
  override fun observeAll()=rows
  override fun observeIsScrapped(apartmentId: String)=rows.map { list -> list.any { it.apartmentId==apartmentId } }
  override suspend fun getAll()=rows.value
  override suspend fun get(apartmentId: String)=rows.value.find { it.apartmentId==apartmentId }
  override suspend fun upsert(scrap: Scrap) { writes++;rows.value=rows.value.filterNot { it.apartmentId==scrap.apartmentId }+scrap }
  override suspend fun delete(apartmentId: String) { error("Voice must not toggle off") }
  override suspend fun markChangeSeen(apartmentId: String) { }
  override suspend fun deleteAll() { rows.value=emptyList() }
 }
 private fun apartments(load: suspend (String)->Outcome<ApartmentDetail>)=object:ApartmentRepository {
  override suspend fun detail(apartmentId: String)=load(apartmentId)
  override suspend fun latestDeal(apartmentId: String): Outcome<Deal> = error("unused")
 }
 @Test fun visibleNudgeExpiresAndHiddenViewsCannotPublish() {
  active();store.showNudge(1,"a",8000)
  assertEquals("a",store.snapshot().apartmentId())
  assertNull(store.snapshot().copy(capturedAt=time.plusSeconds(8)).apartmentId())
  store.setAppState(true,null,false);store.showNudge(2,"b",8000)
  assertNull(store.snapshot().apartmentId())
 }
 @Test fun staleDismissalDoesNotClearReplacementAndDetailWins() {
  active();store.showNudge(1,"a",8000);store.showNudge(2,"b",8000);store.dismissNudge(1)
  assertEquals("b",store.snapshot().apartmentId())
  store.setAppState(true,"selected",true)
  assertEquals("selected",store.snapshot().apartmentId())
 }
 @Test fun foregroundAndConsentInvalidateAcceptedScope() {
  active();store.showNudge(1,"a",8000);val saved=store.snapshot()
  store.setForeground(false);assertFalse(store.isCurrent(saved));assertNull(store.snapshot().apartmentId())
  store.setForeground(true);store.setAppState(true,"a")
  val next=store.snapshot();store.setConsented(false);assertFalse(store.isCurrent(next))
 }
 @Test fun repeatedVoiceAndTapEnsureDoNotDeleteOrRewrite()=runTest {
  active();store.showNudge(1,"a",8000);val rows=Scraps()
  val api=ScrapVoiceTargetUseCase(rows,apartments { Outcome.Success(detail) },store)
  assertTrue(api(call()) is Outcome.Success)
  assertTrue(api(call()) is Outcome.Success)
  assertTrue(api.ensure("a") is Outcome.Success)
  assertEquals(1,rows.writes);assertEquals("a",rows.rows.value.single().apartmentId)
 }
 @Test fun capturedTargetSurvivesReplacementDuringFetch()=runTest {
  active();store.showNudge(1,"a",8000);val captured=call();val rows=Scraps()
  val api=ScrapVoiceTargetUseCase(rows,apartments { id -> assertEquals("a",id);store.showNudge(2,"b",8000);Outcome.Success(detail) },store)
  assertTrue(api(captured) is Outcome.Success);assertEquals("a",rows.rows.value.single().apartmentId)
 }
 @Test fun revokedContextAfterFetchPreventsMutation()=runTest {
  active();store.showNudge(1,"a",8000);val rows=Scraps()
  val api=ScrapVoiceTargetUseCase(rows,apartments { store.setConsented(false);Outcome.Success(detail) },store)
  assertTrue(api(call()) is Outcome.Failure);assertEquals(0,rows.writes)
 }
 @Test fun missingTargetNeverFetchesAndFetchFailureNeverWrites()=runTest {
  active();val rows=Scraps();var fetches=0
  val api=ScrapVoiceTargetUseCase(rows,apartments { fetches++;Outcome.Failure(DomainError.Offline) },store)
  assertEquals(Outcome.Success(ActionResult.NeedsInput("apartment")),api(call()));assertEquals(0,fetches)
  store.showNudge(1,"a",8000);assertEquals(Outcome.Failure(DomainError.Offline),api(call()));assertEquals(0,rows.writes)
 }
}
