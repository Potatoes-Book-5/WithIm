package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.appaction.*

interface AreaPriceSummaryRepository {
    suspend fun get(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<PriceSummaryPayload>>
}
interface InterestAreaRepository {
    suspend fun save(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<InterestAreaSavePayload>>
}
interface DailyBriefingRepository {
    suspend fun get(context: AppCallContext): Outcome<ActionResult<DailyBriefingPayload>>
}
