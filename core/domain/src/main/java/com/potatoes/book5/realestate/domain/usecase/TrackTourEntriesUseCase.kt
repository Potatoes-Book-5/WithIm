package com.potatoes.book5.realestate.domain.usecase

import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.appaction.AppCallContext
import com.potatoes.book5.realestate.domain.model.tour.*
import com.potatoes.book5.realestate.domain.port.*
import java.time.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Foreground-only evidence. Querying membership never calls this mutating observer. */
class TrackTourEntriesUseCase(private val store: TourStore,private val access: TourAccess,
                              private val clock: Clock = Clock.systemUTC()) : TourEntryEvidence {
    private data class State(val armed: Boolean,val entry: AreaEntry?,val lastFix: Instant,val lastEntry: Instant?,val session: String,val revision: Long,val generation: String)
    private val states=ConcurrentHashMap<String,State>()
    private val gate=Mutex()
    private var sequence=0L
    override fun current(profile: String,areaId: String,session: String): AreaEntry? {
        val state=states["$profile|$areaId"] ?: return null
        return state.entry?.takeIf { it.session==session && Duration.between(state.lastFix,clock.instant()).let { age -> !age.isNegative && age<=Duration.ofSeconds(10) } }
    }
    suspend operator fun invoke(context: AppCallContext): Outcome<List<AreaEntry>> = gate.withLock {
        if(!access.observationValid(context)) { states.clear(); return@withLock access.failure() }
        val areas=when(val r=store.areas(context.profileScope)) { is Outcome.Failure -> return@withLock r; is Outcome.Success -> r.value }
        val keys=areas.mapTo(mutableSetOf()) { "${it.profile}|${it.area.id}" };states.keys.retainAll(keys)
        val events=mutableListOf<AreaEntry>();val now=clock.instant()
        for(saved in areas) {
            val key="${saved.profile}|${saved.area.id}";val old=states[key]
            val result=AreaMembershipPolicy.evaluate(saved.area,context.priceLocation,now)
            val observed=context.priceLocation?.observedAt
            val gap=if(observed!=null && old!=null) Duration.between(old.lastFix,observed) else null
            val previous=old?.takeIf { gap!=null && !gap.isNegative && gap<=Duration.ofSeconds(10) &&
                it.session==context.appSessionId && it.revision==context.voice!!.revision && it.generation==saved.generation }
            val last=previous?.lastEntry ?: when(val r=store.latestEntry(saved.profile,saved.area.id)) { is Outcome.Success -> r.value?.at; is Outcome.Failure -> return@withLock r }
            fun state(armed: Boolean,entry: AreaEntry?)=State(armed,entry,observed!!,entry?.at ?: last,context.appSessionId,context.voice!!.revision,saved.generation)
            if(result !is Outcome.Success || observed==null) { states.remove(key);continue }
            if(result.value.state==Membership.UNCERTAIN) {
                if(previous!=null && result.value.reason=="CIRCLE_WITH_ACCURACY") states[key]=state(previous.armed,previous.entry) else states.remove(key)
                continue
            }
            val far=result.value.distanceMeters?.let { it-(context.priceLocation?.accuracyMeters ?: 0.0)>=600 }==true
            if(result.value.state==Membership.OUTSIDE) {
                states[key]=state(far || previous?.armed==true,if(far) null else previous?.entry);continue
            }
            val cooldown=last?.let { Duration.between(it,now) }
            val entry=if(previous?.armed==true && (cooldown==null || (!cooldown.isNegative && cooldown>=Duration.ofMinutes(10))))
                AreaEntry(saved.profile,saved.area.id,context.appSessionId,++sequence,observed,context.voice!!.revision,saved.generation) else previous?.entry
            if(entry!=null && entry!=previous?.entry) {
                if(!access.observationValid(context)) { states.clear(); return@withLock access.failure() }
                when(val r=store.recordEntry(entry) { access.observationValid(context) }) { is Outcome.Failure -> return@withLock r; is Outcome.Success -> events+=entry }
            }
            states[key]=state(false,entry)
        }
        Outcome.Success(events.toList())
    }
}
