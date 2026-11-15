package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.appaction.AppCallContext
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.domain.port.*
import com.potatoes.book5.realestate.domain.usecase.TourAccess
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Polls only while the consented app is foreground/parked. Voice reads never call a source. */
class TourNoticeCollector(private val sources:List<TourNoticeSource>,private val store:TourStore,
    private val prices:PreparedPriceReader,private val access:TourAccess,private val voices:com.potatoes.book5.realestate.domain.model.appaction.VoiceContextPort,private val context:()->AppCallContext,
    private val clock:Clock=Clock.systemUTC()) {
    private data class Cache(val result:Outcome<List<TourNotice>>,val at:Instant)
    private val cache=mutableMapOf<String,Cache>()
    private val gate=kotlinx.coroutines.sync.Mutex()
    suspend fun run() {
        val eligibility=voices.changes.map { it.foreground && it.enabled && it.visualDetailsAllowed }.distinctUntilChanged()
        val ticks=flow { while(currentCoroutineContext().isActive) { emit(Unit);delay(15*60*1000L) } }
        combine(store.observeAreas(context().profileScope),eligibility,ticks) { areas,allowed,_ -> areas.isNotEmpty() && allowed }.collect { allowed ->
            if(allowed) refresh(context())
        }
    }
    suspend fun refresh(context:AppCallContext): Outcome<Unit> {
        if(!gate.tryLock()) return Outcome.Failure(DomainError.Invalid("collection_busy"))
        try {
        if(!access.valid(context)) return access.failure()
        val areas=when(val r=store.areas(context.profileScope)) { is Outcome.Failure -> return r; is Outcome.Success -> r.value }
        if(areas.isEmpty()) return Outcome.Success(Unit)
        for(source in sources) {
            if(!access.valid(context)) return access.failure()
            val cached=cache[source.id]?.takeIf { Duration.between(it.at,clock.instant()).let { age -> !age.isNegative && age<Duration.ofMinutes(15) } }
            val response=cached?.result ?: if(!source.configured) Outcome.Failure(DomainError.MissingApiKey)
                else try { withTimeoutOrNull(30_000) { source.fetch() } ?: Outcome.Failure(DomainError.Timeout) }
                catch(e:CancellationException) { throw e } catch(_:Exception) { Outcome.Failure(DomainError.InvalidResponse) }
            if(!access.valid(context)) return access.failure()
            var state=when(response) {
                is Outcome.Success -> "READY"
                is Outcome.Failure -> when(response.error) { DomainError.MissingApiKey -> "UNCONFIGURED";DomainError.Offline -> "OFFLINE";DomainError.Timeout -> "TIMEOUT";else -> "UNAVAILABLE" }
            }
            val receivedAt=cached?.at ?: clock.instant()
            if(cached==null) cache[source.id]=Cache(response,receivedAt)
            if(response is Outcome.Success) for(area in areas) {
                val district=area.area.districtName ?: prices.knownDistricts().find { it.id==area.area.districtCode }?.name
                if(district==null && response.value.any { it.scope!="NATIONAL" }) state="PARTIAL_AREA_MATCH"
                for(notice in response.value) {
                    val relevant=notice.scope=="NATIONAL" || (district!=null && notice.address?.replace(Regex("\\s+")," ")?.startsWith("$district ")==true)
                    if(relevant) {
                        if(!access.valid(context)) return access.failure()
                        when(val written=store.publishNotice(context.profileScope,area.area.id,notice,receivedAt) { access.valid(context) }) {
                            is Outcome.Failure -> return written
                            is Outcome.Success -> Unit
                        }
                    }
                }
            }
            when(val written=store.setSourceState(context.profileScope,TourSourceState(source.id,state,receivedAt)) { access.valid(context) }) {
                is Outcome.Failure -> return written
                is Outcome.Success -> Unit
            }
        }
        return Outcome.Success(Unit)
        } finally { gate.unlock() }
    }
}
