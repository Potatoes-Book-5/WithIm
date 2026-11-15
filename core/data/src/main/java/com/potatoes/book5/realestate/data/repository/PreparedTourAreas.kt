package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.domain.port.*
import com.potatoes.book5.realestate.domain.usecase.TourAccess
import java.time.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.channels.Channel

/** Explicit place preparation is asynchronous; action requests only read already resolved areas. */
class PreparedTourAreas(private val store: TourStore,private val prices: PreparedPriceReader,
    private val previous: (AppCallContext)->ResolvedArea?, private val access: TourAccess,
    private val lookup: suspend (String)->Outcome<TourArea>,private val clock: Clock = Clock.systemUTC()) : TourAreaResolver {
    private data class Cached(val result: Outcome<TourArea>,val at: Instant)
    private data class Request(val query: String,val context: AppCallContext)
    private val values=AtomicReference<Map<String,Cached>>(emptyMap())
    private val pending=ConcurrentHashMap.newKeySet<String>()
    private val queue=Channel<Request>(8)
    suspend fun run() {
        for(request in queue) try {
            if(!access.valid(request.context)) continue
            val result=lookup(request.query)
            if(!access.valid(request.context)) continue
            values.updateAndGet { old ->
                val next=LinkedHashMap(old);next.remove(request.query);next[request.query]=Cached(result,clock.instant())
                while(next.size>64) next.remove(next.keys.first())
                next.toMap()
            }
        } finally { pending.remove(request.query) }
    }
    override suspend fun resolve(query: String?,context: AppCallContext,preferCurrent: Boolean): Outcome<ActionResult<TourArea>> {
        if(!access.valid(context)) return access.failure()
        val normalized=query?.trim()?.replace(Regex("\\s+")," ")?.takeIf { it.isNotBlank() }
        if(query!=null && (normalized==null || normalized.length>120)) return Outcome.Failure(DomainError.Invalid("invalid_area_query"))
        val target=normalized ?: previous(context)?.name.takeUnless { preferCurrent }
        if(target==null) {
            val fix=context.priceLocation;val area=fix?.area;val at=fix?.observedAt;val accuracy=fix?.accuracyMeters
            if(area==null || at==null || accuracy==null || accuracy !in 0.0..50.0 || Duration.between(at,clock.instant()).let { it.isNegative || it>Duration.ofSeconds(10) })
                return Outcome.Success(ActionResult.NeedsInput("areaQuery"))
            return Outcome.Success(ActionResult.Completed(TourArea("district:${area.id}",area.name,area.id,AreaBoundary.District(area.id),"vworld-point-district",at,area.name)))
        }
        val saved=when(val r=store.areas(context.profileScope)) { is Outcome.Failure -> return r; is Outcome.Success -> r.value.map { it.area } }
        val savedMatches=saved.filter { it.id==target || it.name==target }
        if(savedMatches.size==1) return Outcome.Success(ActionResult.Completed(savedMatches.single()))
        if(savedMatches.size>1) return Outcome.Success(ActionResult.NeedsInput("areaQuery"))
        val districts=prices.knownDistricts().filter { it.name==target || it.name.substringAfterLast(' ')==target }
        if(districts.size==1) {
            val d=districts.single()
            return Outcome.Success(ActionResult.Completed(TourArea("district:${d.id}",d.name,d.id,AreaBoundary.District(d.id),"vworld-district-catalog",clock.instant(),d.name)))
        }
        if(districts.size>1) return Outcome.Success(ActionResult.NeedsInput("areaQuery"))
        val cached=values.get()[target]
        if(cached!=null && Duration.between(cached.at,clock.instant()).let { !it.isNegative && it < if(cached.result is Outcome.Success) Duration.ofDays(1) else Duration.ofSeconds(30) })
            return when(val result=cached.result) {
                is Outcome.Failure -> if(result.error is DomainError.Invalid) Outcome.Success(ActionResult.NeedsInput("areaQuery")) else result
                is Outcome.Success -> Outcome.Success(ActionResult.Completed(result.value))
            }
        if(pending.add(target) && !queue.trySend(Request(target,context)).isSuccess) pending.remove(target)
        return Outcome.Failure(DomainError.DataNotReady(PreparedDataFailure.NOT_READY))
    }
}
