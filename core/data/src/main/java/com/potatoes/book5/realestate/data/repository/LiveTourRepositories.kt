package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.domain.port.*
import com.potatoes.book5.realestate.domain.usecase.*
import java.time.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect

class LiveInterestAreaRepository(private val save: SaveTourAreaUseCase,
                                  private val previous: (AppCallContext)->ResolvedArea?) : InterestAreaRepository {
    override suspend fun save(target: AreaTarget,context: AppCallContext): Outcome<ActionResult<InterestAreaSavePayload>> {
        val query=when(target.kind) {
            AreaTargetKind.CURRENT_LOCATION -> null
            AreaTargetKind.NAMED_AREA -> target.query
            AreaTargetKind.LAST_REFERENCED_AREA -> previous(context)?.name ?: return Outcome.Success(ActionResult.NeedsInput("areaQuery"))
        }
        return when(val r=save(query,context,target.kind==AreaTargetKind.CURRENT_LOCATION)) {
            is Outcome.Failure -> r
            is Outcome.Success -> when(val value=r.value) {
                is ActionResult.NeedsInput -> Outcome.Success(value)
                is ActionResult.Completed -> {
                    val saved=value.payload
                    Outcome.Success(ActionResult.Completed(InterestAreaSavePayload(ResolvedArea(saved.saved.area.id,saved.saved.area.name),true,
                        if(saved.alreadySaved) MutationState.ALREADY_EXISTS else MutationState.COMMITTED,
                        ResultMetadata(ExecutionMode.LOCAL,false,"local-interest-areas",saved.saved.savedAt.toString()))))
                }
            }
        }
    }
}

class LiveDailyBriefingRepository(private val store: TourStore,private val prices: PreparedPriceReader,
                                  private val access: TourAccess,private val sources:List<TourNoticeSource> = emptyList(),private val clock:Clock=Clock.systemUTC()) : DailyBriefingRepository {
    override suspend fun get(context: AppCallContext): Outcome<ActionResult<DailyBriefingPayload>> {
        if(!access.valid(context)) return access.failure()
        val areas=when(val r=store.areas(context.profileScope)) { is Outcome.Failure -> return r; is Outcome.Success -> r.value }
        if(areas.isEmpty()) return Outcome.Success(ActionResult.NeedsInput("interestArea"))
        val unread=when(val r=store.unread(context.profileScope)) { is Outcome.Failure -> return r; is Outcome.Success -> r.value }
        val ready=areas.all { prices.readDistrictPrices(it.area.districtCode) is Outcome.Success }
        val notices=when(val r=store.unreadNotices(context.profileScope)) { is Outcome.Failure -> return r; is Outcome.Success -> r.value }
        val states=when(val r=store.sourceStates(context.profileScope)) { is Outcome.Failure -> return r; is Outcome.Success -> r.value }
        val status=mutableMapOf("SALE_SUMMARY" to if(ready) "READY" else if(unread.isNotEmpty()) "CACHED" else "NOT_READY",
            "SUBSCRIPTION" to "UNSUPPORTED","NEWS" to "UNSUPPORTED","AUTO_BRIEFING" to "UNSUPPORTED")
        for(source in sources) {
            val state=states.find { it.source==source.id }
            val fresh=state?.let { Duration.between(it.attemptedAt,clock.instant()).let { age -> !age.isNegative && age<Duration.ofHours(1) } }==true
            status[source.id]=when { !source.configured -> "UNCONFIGURED";fresh -> state!!.state; notices.any { it.notice.source==source.id } -> "CACHED";else -> "NOT_READY" }
        }
        val summaries=unread.map { row -> BriefingItem("price-summary:${row.areaId}","PRICE_SUMMARY_UPDATE",null,
            "Previously collected district-wide sale summary for ${row.areaName}: ${row.sampleCount} samples, average KRW ${row.averagePriceKrw ?: "unavailable"}. This is a collected aggregate update, not a new contract or station-area average.",
            row.observedAt.toString(),row.areaId,row.version,sourceId="molit-apartment-sale",scope="DISTRICT") }
        val sourceItems=notices.map { row -> BriefingItem(row.notice.id,row.notice.type,
            row.notice.publishedAt?.toString() ?: row.notice.publishedDate?.toString(),
            if(row.notice.scope=="NATIONAL") "National government press release: ${row.notice.title}" else "Published housing announcement: ${row.notice.title}",
            row.observedAt.toString(),null,row.version,row.notice.url,row.notice.source,row.notice.scope) }
        val all=summaries+sourceItems
        val distinct=all.distinctBy { it.type }.take(3)
        val items=(distinct+all.filterNot { it.id in distinct.map { selected -> selected.id } }).take(3)
        val latest=when(val r=store.areas(context.profileScope)) { is Outcome.Failure -> return r; is Outcome.Success -> r.value }
        if(latest.associate { it.area.id to it.generation }!=areas.associate { it.area.id to it.generation })
            return Outcome.Failure(DomainError.Invalid("areas_changed"))
        if(!access.valid(context)) return access.failure()
        return Outcome.Success(ActionResult.Completed(DailyBriefingPayload(items,
            ResultMetadata(ExecutionMode.LIVE,false,"local-molit-summary-history",unread.maxOfOrNull { it.sourceAsOf }?.toString()),status)))
    }
}

/** Publishes actual collector changes, not fetch timestamps or invented individual trade IDs. */
class TourBriefingCollector(private val store: TourStore,private val prices: InMemoryPreparedMarket,
                            private val profile: String,private val clock: Clock = Clock.systemUTC()) {
    suspend fun run() {
        combine(store.observeAreas(profile),prices.changes) { areas,_ -> areas }.collect { areas ->
            for(saved in areas) when(val r=prices.readDistrictPrices(saved.area.districtCode)) {
                is Outcome.Failure -> Unit
                is Outcome.Success -> store.publishSummary(profile,saved.area.id,r.value,clock.instant())
            }
        }
    }
}
