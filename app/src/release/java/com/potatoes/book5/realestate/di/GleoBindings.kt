package com.potatoes.book5.realestate.di
import android.content.Context
import com.potatoes.book5.realestate.vehicle.gleo.GleoAndroidHandler
internal object GleoBindings {
 @Suppress("UNUSED_PARAMETER") fun create(context: Context, scrap: com.potatoes.book5.realestate.domain.usecase.ScrapVoiceTargetUseCase,
    prices: com.potatoes.book5.realestate.domain.port.AreaPriceSummaryRepository,
    areas: com.potatoes.book5.realestate.domain.port.InterestAreaRepository,
    briefing: com.potatoes.book5.realestate.domain.port.DailyBriefingRepository,
    captureContext: () -> com.potatoes.book5.realestate.domain.model.appaction.AppCallContext): GleoAndroidHandler? = null
}
