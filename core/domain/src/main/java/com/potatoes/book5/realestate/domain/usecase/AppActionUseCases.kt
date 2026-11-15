package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.port.AreaPriceSummaryRepository
import com.potatoes.book5.realestate.domain.port.InterestAreaRepository
import com.potatoes.book5.realestate.domain.port.DailyBriefingRepository

private fun validContext(context: AppCallContext): Boolean =
    context.requestId.isNotBlank() && context.appSessionId.isNotBlank() && context.profileScope.isNotBlank()

private suspend fun <T> withValidTarget(
    target: AreaTarget,
    context: AppCallContext,
    operation: suspend () -> Outcome<ActionResult<T>>,
): Outcome<ActionResult<T>> = when {
    !validContext(context) -> Outcome.Failure(DomainError.Invalid("Missing application context"))
    target.kind == AreaTargetKind.NAMED_AREA && target.query.isNullOrBlank() ->
        Outcome.Success(ActionResult.NeedsInput("areaQuery"))
    target.kind != AreaTargetKind.NAMED_AREA && target.query != null ->
        Outcome.Failure(DomainError.Invalid("Area query conflicts with target"))
    else -> operation()
}

class GetAreaPriceSummaryUseCase(
    private val repository: AreaPriceSummaryRepository,
    private val contextualTargetsAllowed: Boolean = true,
) {
    suspend operator fun invoke(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<PriceSummaryPayload>> =
        withValidTarget(target, context) {
            if (!contextualTargetsAllowed && target.kind != AreaTargetKind.NAMED_AREA)
                Outcome.Failure(DomainError.Unsupported("authenticated_location_route"))
            else repository.get(target, context)
        }
}
class SaveInterestAreaUseCase(private val repository: InterestAreaRepository) {
    suspend operator fun invoke(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<InterestAreaSavePayload>> =
        withValidTarget(target, context) { repository.save(target, context) }
}
class GetDailyBriefingUseCase(private val repository: DailyBriefingRepository) {
    suspend operator fun invoke(context: AppCallContext): Outcome<ActionResult<DailyBriefingPayload>> =
        if (validContext(context)) repository.get(context)
        else Outcome.Failure(DomainError.Invalid("Missing application context"))
}
