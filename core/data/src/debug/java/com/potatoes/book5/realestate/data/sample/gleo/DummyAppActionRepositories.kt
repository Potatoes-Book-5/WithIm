package com.potatoes.book5.realestate.data.sample.gleo

import com.potatoes.book5.realestate.domain.error.DomainError
import com.potatoes.book5.realestate.domain.error.Outcome
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.port.AreaPriceSummaryRepository
import com.potatoes.book5.realestate.domain.port.InterestAreaRepository
import com.potatoes.book5.realestate.domain.port.DailyBriefingRepository

private fun metadata(source: String = "synthetic-sale-provider") =
    ResultMetadata(ExecutionMode.DUMMY, true, source, "2026-11-14")

private val sampleArea = ResolvedArea("synthetic-area-a", "가상마을 A")

private fun resolve(target: AreaTarget, lastArea: ResolvedArea?): Outcome<ActionResult<ResolvedArea>> = when (target.kind) {
    AreaTargetKind.CURRENT_LOCATION -> Outcome.Success(ActionResult.Completed(sampleArea))
    AreaTargetKind.LAST_REFERENCED_AREA -> if (lastArea != null) Outcome.Success(ActionResult.Completed(lastArea))
        else Outcome.Success(ActionResult.NeedsInput("areaQuery"))
    AreaTargetKind.NAMED_AREA -> if (target.query?.trim() == sampleArea.name) Outcome.Success(ActionResult.Completed(sampleArea))
        else Outcome.Failure(DomainError.Unsupported("real_area_resolution"))
}

private inline fun <T> forArea(
    target: AreaTarget,
    lastArea: ResolvedArea?,
    build: (ResolvedArea) -> T,
): Outcome<ActionResult<T>> = when (val result = resolve(target, lastArea)) {
    is Outcome.Failure -> result
    is Outcome.Success -> when (val value = result.value) {
        is ActionResult.NeedsInput -> Outcome.Success(value)
        is ActionResult.Completed -> Outcome.Success(ActionResult.Completed(build(value.payload)))
    }
}

/** No database, location, preference or network dependencies. */
class DummyAreaPriceSummaryRepository(private val lastArea: ResolvedArea? = null) : AreaPriceSummaryRepository {
    override suspend fun get(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<PriceSummaryPayload>> =
        forArea(target, lastArea) { area -> PriceSummaryPayload(area, 1_200_000_000L, 1_550_000_000L, "C", "가상 푸른아파트",
            5, 80, 90, "2026-08-17", "2026-11-15", metadata()) }
}
class DummyInterestAreaRepository(private val lastArea: ResolvedArea? = null) : InterestAreaRepository {
    override suspend fun save(target: AreaTarget, context: AppCallContext): Outcome<ActionResult<InterestAreaSavePayload>> =
        forArea(target, lastArea) { area -> InterestAreaSavePayload(area, false, MutationState.NOT_APPLIED, metadata()) }
}
class DummyDailyBriefingRepository : DailyBriefingRepository {
    override suspend fun get(context: AppCallContext): Outcome<ActionResult<DailyBriefingPayload>> =
        Outcome.Success(ActionResult.Completed(DailyBriefingPayload(listOf(
            BriefingItem("synthetic-sale-new", "SALE", "2026-11-14T08:00:00+09:00", "Synthetic published sale update."),
            BriefingItem("synthetic-subscription-1", "SUBSCRIPTION", "2026-11-14T08:00:00+09:00", "Synthetic subscription announcement."),
            BriefingItem("synthetic-news-1", "NEWS", "2026-11-14T08:00:00+09:00", "Synthetic news item."),
        ), metadata("synthetic-briefing-provider"))))
}
