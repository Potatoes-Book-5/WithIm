package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.domain.model.appaction.AppCallContext
import com.potatoes.book5.realestate.domain.usecase.GetAreaPriceSummaryUseCase
import com.potatoes.book5.realestate.domain.usecase.SaveInterestAreaUseCase
import com.potatoes.book5.realestate.domain.usecase.GetDailyBriefingUseCase
import kotlinx.coroutines.CancellationException

class GleoIntentAdapter(
    private val getPrice: GetAreaPriceSummaryUseCase,
    private val saveArea: SaveInterestAreaUseCase,
    private val getBriefing: GetDailyBriefingUseCase,
    private val sender: GleoCallbackSender,
    private val contextFactory: () -> AppCallContext,
    private val coordinator: GleoRequestCoordinator = GleoRequestCoordinator(),
    private val validator: GleoRequestValidator = GleoRequestValidator(),
    private val mapper: GleoResponseMapper = GleoResponseMapper(),
    private val scrapTarget: com.potatoes.book5.realestate.domain.usecase.ScrapVoiceTargetUseCase? = null,
) {
    fun captureContext() = contextFactory()
    suspend fun handle(input: GleoInput, capturedContext: AppCallContext? = null) = coordinator.execute(input.action, sender, input.receivedAtMillis) {
        try {
            when (val validation = validator.validate(input)) {
                is GleoValidation.Invalid -> validation.requiredField?.let { GleoCallback.asking(input.action, it) }
                    ?: GleoCallback.failure(input.action, validation.code)
                is GleoValidation.Valid -> {
                    val request = validation.request
                    val context = capturedContext ?: contextFactory()
                    when (request.action) {
                        GleoAction.GET_AREA_PRICE_SUMMARY -> mapper.map(request.action, getPrice(requireNotNull(request.target), context))
                        GleoAction.SAVE_INTEREST_AREA -> mapper.map(request.action, saveArea(requireNotNull(request.target), context))
                        GleoAction.GET_REAL_ESTATE_BRIEFING -> mapper.map(request.action, getBriefing(context))
                        GleoAction.SCRAP_APARTMENT -> scrapTarget?.let { mapper.mapScrap(it(context)) }
                            ?: GleoCallback.failure(request.action, "UNSUPPORTED")
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            GleoCallback.failure(input.action, "INTERNAL_ERROR")
        }
    }
}
