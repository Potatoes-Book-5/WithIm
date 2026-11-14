package com.potatoes.book5.realestate.domain.model.appaction

enum class AreaTargetKind { CURRENT_LOCATION, LAST_REFERENCED_AREA, NAMED_AREA }
data class AreaTarget(val kind: AreaTargetKind, val query: String? = null)
data class AppCallContext(val requestId: String, val appSessionId: String, val profileScope: String)
data class ResolvedArea(val id: String, val name: String)
enum class ExecutionMode { DUMMY }
data class ResultMetadata(
    val executionMode: ExecutionMode,
    val synthetic: Boolean,
    val sourceId: String,
    val sourceAsOf: String,
)
sealed interface ActionResult<out T> {
    data class Completed<T>(val payload: T) : ActionResult<T>
    data class NeedsInput(val field: String) : ActionResult<Nothing>
}
sealed interface AppActionPayload { val metadata: ResultMetadata }
data class PriceSummaryPayload(
    val area: ResolvedArea,
    val averagePriceKrw: Long,
    val highestPriceKrw: Long,
    val highestComplexId: String,
    val highestComplexName: String,
    val sampleCount: Int,
    val areaMinM2: Int,
    val areaMaxExclusiveM2: Int,
    val periodStart: String,
    val periodEndExclusive: String,
    override val metadata: ResultMetadata,
) : AppActionPayload
enum class MutationState { NOT_APPLIED }
data class InterestAreaSavePayload(
    val area: ResolvedArea,
    val persisted: Boolean,
    val mutationState: MutationState,
    override val metadata: ResultMetadata,
) : AppActionPayload
data class BriefingItem(val id: String, val type: String, val publishedAt: String, val summary: String)
data class DailyBriefingPayload(
    val items: List<BriefingItem>,
    override val metadata: ResultMetadata,
) : AppActionPayload
