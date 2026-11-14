package com.potatoes.book5.realestate.di
import android.content.Context
import com.potatoes.book5.realestate.data.sample.gleo.*
import com.potatoes.book5.realestate.domain.model.appaction.AppCallContext
import com.potatoes.book5.realestate.domain.usecase.*
import com.potatoes.book5.realestate.vehicle.gleo.*
import java.util.UUID
internal object GleoBindings {
 fun create(context: Context): GleoAndroidHandler {
  val session = UUID.randomUUID().toString()
  return GleoAndroidHandler(GleoIntentAdapter(
   GetAreaPriceSummaryUseCase(DummyAreaPriceSummaryRepository()),
   SaveInterestAreaUseCase(DummyInterestAreaRepository()),
   GetDailyBriefingUseCase(DummyDailyBriefingRepository()),
   BroadcastGleoCallbackSender(context, responseAction = BroadcastGleoCallbackSender.GLEO_RESPONSE_ACTION),
   { AppCallContext(UUID.randomUUID().toString(), session, "synthetic-profile") }
  ))
 }
}
