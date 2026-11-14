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
 private val apartments: ApartmentRepository,
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
  val detail=when(val result=apartments.detail(id)) {
   is Outcome.Failure -> return@withLock result
   is Outcome.Success -> result.value
  }
  if(detail.id!=id) return@withLock Outcome.Failure(DomainError.InvalidResponse)
  if(!allowed()) return@withLock Outcome.Failure(DomainError.Invalid("context_invalidated"))
  // Recheck after fetch so another completed save is never overwritten.
  if(scraps.get(id)!=null) return@withLock Outcome.Success(ActionResult.Completed(VoiceScrapResult(id,true)))
  val at=now(); val record=PriceRecord(detail.latestDeal.priceManwon,detail.latestDeal.date)
  scraps.upsert(Scrap(id,detail.name,detail.neighborhood,detail.latestDeal.areaPyeong,at,record,record,at,null,detail.households,detail.builtYear))
  Outcome.Success(ActionResult.Completed(VoiceScrapResult(id,false)))
 }
}
