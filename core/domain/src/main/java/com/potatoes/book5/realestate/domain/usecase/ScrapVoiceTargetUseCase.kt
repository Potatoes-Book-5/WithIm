package com.potatoes.book5.realestate.domain.usecase
import com.potatoes.book5.realestate.domain.error.*
import com.potatoes.book5.realestate.domain.model.*
import com.potatoes.book5.realestate.domain.model.appaction.*
import com.potatoes.book5.realestate.domain.port.*
import java.time.LocalDateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class VoiceScrapResult(val apartmentId: String, val alreadySaved: Boolean)
/** Ensure-saved operation: a repeated voice request must never toggle a scrap off. */
class ScrapVoiceTargetUseCase(
 private val scraps: ScrapRepository,
 private val prepared: PreparedScrapReader,
 private val contexts: VoiceContextPort,
 private val now: () -> LocalDateTime = LocalDateTime::now,
) {
 private val mutex = Mutex()
 suspend operator fun invoke(context: AppCallContext): Outcome<ActionResult<VoiceScrapResult>> {
  val snapshot=context.voice ?: return Outcome.Success(ActionResult.NeedsInput("apartment"))
  val id=snapshot.apartmentId() ?: return Outcome.Success(ActionResult.NeedsInput("apartment"))
  return save(id) { contexts.isCurrent(snapshot) }
 }
 /** Banner taps share the same serialized ensure-saved operation as voice. */
 suspend fun ensure(apartmentId: String) = save(apartmentId) { true }
 private suspend fun save(id: String, allowed: () -> Boolean): Outcome<ActionResult<VoiceScrapResult>> = mutex.withLock {
  if(!allowed()) return@withLock Outcome.Failure(DomainError.Invalid("context_invalidated"))
  if(scraps.get(id)!=null) return@withLock Outcome.Success(ActionResult.Completed(VoiceScrapResult(id,true)))
  val candidate=when(val result=prepared.readScrapCandidate(id)) {
   is Outcome.Failure -> return@withLock result
   is Outcome.Success -> result.value
  }
  if(candidate.apartmentId!=id || candidate.name.isBlank() || candidate.areaPyeong <= 0 || candidate.latest.priceManwon <= 0) return@withLock Outcome.Failure(DomainError.InvalidResponse)
  if(!allowed()) return@withLock Outcome.Failure(DomainError.Invalid("context_invalidated"))
  // Recheck after fetch so another completed save is never overwritten.
  if(scraps.get(id)!=null) return@withLock Outcome.Success(ActionResult.Completed(VoiceScrapResult(id,true)))
  val at=now(); val record=candidate.latest
  scraps.upsert(Scrap(id,candidate.name,candidate.neighborhood,candidate.areaPyeong,at,record,record,at,null,candidate.households,candidate.builtYear))
  Outcome.Success(ActionResult.Completed(VoiceScrapResult(id,false)))
 }
}
