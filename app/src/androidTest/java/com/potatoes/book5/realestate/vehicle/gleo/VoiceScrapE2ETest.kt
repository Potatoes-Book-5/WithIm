package com.potatoes.book5.realestate.vehicle.gleo
import android.content.*
import androidx.core.content.ContextCompat
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.potatoes.book5.realestate.data.local.RoomScrapRepository
import com.potatoes.book5.realestate.data.local.db.AppDatabase
import com.potatoes.book5.realestate.data.repository.InMemoryVoiceContext
import com.potatoes.book5.realestate.data.sample.gleo.*
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.port.ApartmentRepository
import com.potatoes.book5.realestate.domain.usecase.*
import java.time.LocalDate
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
class VoiceScrapE2ETest {
 @Test fun nudgeIntentFetchRoomCallbackAndRetry() = runBlocking {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
  val store=InMemoryVoiceContext()
  store.setConsented(true);store.setForeground(true);store.setAppState(true,null,true)
  store.showNudge(42,"synthetic-voice-e2e",8000)
  val rows=RoomScrapRepository(db.scrapDao())
  val detail=ApartmentDetail("synthetic-voice-e2e","Synthetic E2E","d","n",null,null,null,null,null,
   Deal(LocalDate.of(2026,11,14),30,1,10000),null,emptyList(),emptyList())
  var fetches=0
  val apartments=object:ApartmentRepository {
   override suspend fun detail(apartmentId: String): Outcome<ApartmentDetail> { fetches++; assertEquals(detail.id,apartmentId); return Outcome.Success(detail) }
   override suspend fun latestDeal(apartmentId: String): Outcome<Deal> = error("unused")
  }
  val queue=LinkedBlockingQueue<Intent>()
  val receiver=object:BroadcastReceiver() { override fun onReceive(c:Context,i:Intent) { queue.offer(i) } }
  ContextCompat.registerReceiver(context,receiver,IntentFilter(BroadcastGleoCallbackSender.GLEO_RESPONSE_ACTION),ContextCompat.RECEIVER_EXPORTED)
  try {
   val handler=GleoAndroidHandler(GleoIntentAdapter(
    GetAreaPriceSummaryUseCase(DummyAreaPriceSummaryRepository()),SaveInterestAreaUseCase(DummyInterestAreaRepository()),
    GetDailyBriefingUseCase(DummyDailyBriefingRepository()),
    BroadcastGleoCallbackSender(context,responseAction=BroadcastGleoCallbackSender.GLEO_RESPONSE_ACTION),
    { AppCallContext("r","session","test",store.snapshot()) },scrapTarget=ScrapVoiceTargetUseCase(rows,apartments,store)))
   handler.handle(Intent("SCRAP_APARTMENT"))
   var response=JSONObject(queue.poll(5,TimeUnit.SECONDS)!!.getStringExtra("response")!!)
   assertEquals("success",response.getString("status"))
   assertTrue(JSONArray(response.getString("data")).getJSONObject(0).getBoolean("persisted"))
   assertEquals(detail.id,rows.getAll().single().apartmentId)
   handler.handle(Intent("SCRAP_APARTMENT"))
   response=JSONObject(queue.poll(5,TimeUnit.SECONDS)!!.getStringExtra("response")!!)
   assertTrue(JSONArray(response.getString("data")).getJSONObject(0).getBoolean("alreadySaved"))
   assertEquals(1,fetches)
   store.dismissNudge(42)
   handler.handle(Intent("SCRAP_APARTMENT"))
   response=JSONObject(queue.poll(5,TimeUnit.SECONDS)!!.getStringExtra("response")!!)
   assertEquals("asking",response.getString("status"))
   assertEquals(1,rows.getAll().size)
  } finally { context.unregisterReceiver(receiver);db.close() }
 }
}
