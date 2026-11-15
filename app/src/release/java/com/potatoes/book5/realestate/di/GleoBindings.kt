package com.potatoes.book5.realestate.di
import android.content.Context
import com.potatoes.book5.realestate.vehicle.gleo.GleoAndroidHandler
internal object GleoBindings {
 @Suppress("UNUSED_PARAMETER") fun create(context: Context, voiceContext: com.potatoes.book5.realestate.domain.model.appaction.VoiceContextPort, scrap: com.potatoes.book5.realestate.domain.usecase.ScrapVoiceTargetUseCase,
    prices: com.potatoes.book5.realestate.domain.port.AreaPriceSummaryRepository,
    captureLocation: () -> com.potatoes.book5.realestate.domain.model.appaction.PriceLocationSnapshot): GleoAndroidHandler? = null
}
