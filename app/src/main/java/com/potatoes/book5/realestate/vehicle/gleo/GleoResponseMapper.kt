package com.potatoes.book5.realestate.vehicle.gleo

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.appaction.*

class GleoResponseMapper {
    fun map(action: GleoAction, outcome: Outcome<ActionResult<AppActionPayload>>): GleoCallback = when (outcome) {
        is Outcome.Failure -> GleoCallback.failure(action, when (val error = outcome.error) {
            is DomainError.DataNotReady -> if (error.reason == com.potatoes.book5.realestate.domain.error.PreparedDataFailure.STALE) "STALE_DATA" else "DATA_NOT_READY"
            DomainError.Offline -> "OFFLINE"
            DomainError.Timeout -> "TIMEOUT"
            DomainError.MissingApiKey -> "SERVICE_UNAVAILABLE"
            DomainError.InvalidResponse -> "INVALID_PROVIDER_RESPONSE"
            DomainError.ProviderRejected -> "PROVIDER_REJECTED"
            is DomainError.Http -> "PROVIDER_ERROR"
            is DomainError.Unsupported -> "UNSUPPORTED"
            is DomainError.Invalid -> "INVALID_REQUEST"
            is DomainError.Unknown -> "INTERNAL_ERROR"
        })
        is Outcome.Success -> when (val result = outcome.value) {
            is ActionResult.NeedsInput -> GleoCallback.asking(action, result.field)
            is ActionResult.Completed -> completed(action, result.payload)
        }
    }

    fun mapScrap(outcome: Outcome<ActionResult<com.potatoes.book5.realestate.domain.usecase.VoiceScrapResult>>, includeApartmentId: Boolean = true): GleoCallback {
        val action = GleoAction.SCRAP_APARTMENT
        return when(outcome) {
            is Outcome.Failure -> map(action, outcome).let { callback -> callback.copy(data=callback.data.map { it + mapOf("executionMode" to "LOCAL", "synthetic" to false) }) }
            is Outcome.Success -> when(val result=outcome.value) {
                is ActionResult.NeedsInput -> GleoCallback.asking(action,result.field)
                is ActionResult.Completed -> GleoCallback(action,"success","The apartment is saved in your scraps.", listOf(
                    mapOf("persisted" to true,"alreadySaved" to result.payload.alreadySaved,"executionMode" to "LOCAL") +
                        if (includeApartmentId) mapOf("apartmentId" to result.payload.apartmentId) else emptyMap()
                ))
            }
        }
    }
    private fun completed(action: GleoAction, payload: AppActionPayload): GleoCallback {
        val meta = payload.metadata
        val common = mapOf<String, Any?>("executionMode" to meta.executionMode.name, "synthetic" to meta.synthetic,
            "sourceId" to meta.sourceId, "sourceAsOf" to meta.sourceAsOf)
        val fields: Map<String, Any?> = when (payload) {
            is PriceSummaryPayload -> mapOf(
                "areaId" to payload.area.id, "areaName" to payload.area.name, "transactionType" to "SALE",
                "averagePriceKrw" to payload.averagePriceKrw, "highestPriceKrw" to payload.highestPriceKrw,
                "highestComplexId" to payload.highestComplexId, "highestComplexName" to payload.highestComplexName,
                "sampleCount" to payload.sampleCount, "areaMinM2" to payload.areaMinM2,
                "areaMaxExclusiveM2" to payload.areaMaxExclusiveM2, "periodStart" to payload.periodStart,
                "periodEndExclusive" to payload.periodEndExclusive,
                "scope" to payload.scope,
                "sampleStatus" to when { payload.sampleCount == 0 -> "EMPTY"; payload.sampleCount < 3 -> "INSUFFICIENT"; else -> "AVAILABLE" },
                "highestComplexIdentified" to (payload.highestComplexId != null),
                "locationObservedAt" to payload.locationObservedAt,
                "locationAccuracyMeters" to payload.locationAccuracyMeters,
                "locationSource" to payload.locationSource,
            )
            is InterestAreaSavePayload -> mapOf("areaId" to payload.area.id, "areaName" to payload.area.name,
                "persisted" to payload.persisted, "mutationState" to payload.mutationState.name,
                "code" to if(payload.mutationState==MutationState.NOT_APPLIED) "DUMMY_NOT_PERSISTED" else if(payload.mutationState==MutationState.ALREADY_EXISTS) "ALREADY_SAVED" else "AREA_SAVED")
            is DailyBriefingPayload -> emptyMap()
        }
        val data = if (payload is DailyBriefingPayload) listOf(common + mapOf(
            "sourceStates" to payload.sourceStates, "itemCount" to payload.items.size,
            "items" to payload.items.map { mapOf("id" to it.id,"type" to it.type,"publishedAt" to it.publishedAt,
                "summary" to it.summary,"observedAt" to it.observedAt,"areaId" to it.areaId,"version" to it.version,"url" to it.url,"sourceId" to it.sourceId,"scope" to it.scope) }
        )) else listOf(common + fields)
        return if (payload is InterestAreaSavePayload && !payload.persisted) GleoCallback(action, "fail",
            "This is a test response. No area was saved.", data)
        else GleoCallback(action, "success", when {
            payload is InterestAreaSavePayload -> "The interest area has been saved locally. Repeated requests keep the existing area."
            meta.synthetic -> "This is synthetic test data, not real market information."
            payload is PriceSummaryPayload && payload.sampleCount == 0 -> "No published sales match this district, period and area range. Prices are unavailable, not zero."
            payload is PriceSummaryPayload && payload.sampleCount < 3 -> "Published district sales are available, but fewer than three samples are insufficient to describe a typical market price. State the sample count and source date."
            payload is DailyBriefingPayload -> "Saved-area aggregate updates only. Source states identify unavailable announcements and news; empty items do not prove those sources have no updates."
            else -> "These are previously collected published district sales, not live asking prices. State the district-wide scope, period, area range, sample count and collection time."
        }, data)
    }
}
