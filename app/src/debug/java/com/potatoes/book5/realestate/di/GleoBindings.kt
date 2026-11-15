package com.potatoes.book5.realestate.di
import android.content.Context
import com.potatoes.book5.realestate.data.repository.UnavailableInterestAreaRepository
import com.potatoes.book5.realestate.data.repository.UnavailableDailyBriefingRepository
import com.potatoes.book5.realestate.domain.model.appaction.AppCallContext
import com.potatoes.book5.realestate.domain.model.appaction.PriceLocationSnapshot
import com.potatoes.book5.realestate.domain.port.AreaPriceSummaryRepository
import com.potatoes.book5.realestate.domain.usecase.*
import com.potatoes.book5.realestate.vehicle.gleo.*
import java.util.UUID
internal object GleoBindings {
 fun create(context: Context, voiceContext: com.potatoes.book5.realestate.domain.model.appaction.VoiceContextPort, scrap: com.potatoes.book5.realestate.domain.usecase.ScrapVoiceTargetUseCase,
            prices: AreaPriceSummaryRepository, captureLocation: () -> PriceLocationSnapshot): GleoAndroidHandler {
  val session = UUID.randomUUID().toString()
  return GleoAndroidHandler(GleoIntentAdapter(
   GetAreaPriceSummaryUseCase(prices, contextualTargetsAllowed = false),
   SaveInterestAreaUseCase(UnavailableInterestAreaRepository()),
   GetDailyBriefingUseCase(UnavailableDailyBriefingRepository()),
   object : GleoCallbackSender {
    private val delegate = BroadcastGleoCallbackSender(context, responseAction = BroadcastGleoCallbackSender.GLEO_RESPONSE_ACTION)
    override suspend fun send(callback: GleoCallback) {
     delegate.send(callback)
     // Fixed labels only. Never log request payloads, locations, identifiers or response text.
     val status = callback.status.takeIf { it in setOf("success", "asking", "fail") } ?: "unknown"
     android.util.Log.i("GleoE2E", "event=callback_sent action=${callback.action.name} status=$status persisted=${callback.data.firstOrNull()?.get("persisted") == true} ")
    }
   },
   { AppCallContext(UUID.randomUUID().toString(), session, "local-device-scope", voiceContext.snapshot(), captureLocation()) },
   scrapTarget = scrap
  ))
 }
}
