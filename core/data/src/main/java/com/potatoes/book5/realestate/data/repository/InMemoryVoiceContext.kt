package com.potatoes.book5.realestate.data.repository
import com.potatoes.book5.realestate.domain.model.appaction.*
import java.time.Clock
/** Process-local trusted UI context. No raw utterance, location or persistence. */
class InMemoryVoiceContext(private val clock: Clock = Clock.systemUTC()) : VoiceContextPort {
 private val updates=kotlinx.coroutines.flow.MutableStateFlow(VoiceContextSnapshot())
 override val changes: kotlinx.coroutines.flow.Flow<VoiceContextSnapshot> get()=updates
 private var value = VoiceContextSnapshot()
  set(new) { field=new;updates.value=new }
 private var appEnabled = false
 private var consented = false
 private var nudgeVisible = false
 @Synchronized override fun snapshot() = value.copy(capturedAt = clock.instant())
 @Synchronized override fun isCurrent(snapshot: VoiceContextSnapshot) =
  value.revision == snapshot.revision && value.foreground && value.enabled
 @Synchronized override fun setForeground(value: Boolean) {
  if (this.value.foreground != value) this.value = this.value.copy(
   foreground=value, revision=this.value.revision+1, nudge=null)
 }
 @Synchronized override fun setConsented(value: Boolean) {
  if(consented != value) {
   consented=value
   this.value=this.value.copy(enabled=appEnabled && value, revision=this.value.revision+1, nudge=null)
  }
 }
 @Synchronized override fun setAppState(enabled: Boolean, selectedApartmentId: String?, nudgeVisible: Boolean, visualDetailsAllowed: Boolean) {
  this.nudgeVisible=nudgeVisible
  appEnabled=enabled
  val allowed=enabled && consented
  value = value.copy(enabled=allowed, selectedApartmentId=selectedApartmentId,
   revision=value.revision + if (allowed != value.enabled || visualDetailsAllowed != value.visualDetailsAllowed) 1 else 0,
   visualDetailsAllowed=visualDetailsAllowed,
   nudge=if(allowed && nudgeVisible) value.nudge else null)
 }
 @Synchronized override fun showNudge(key: Long, apartmentId: String, durationMillis: Long) {
  if (nudgeVisible && value.foreground && value.enabled && apartmentId.isNotBlank() && durationMillis > 0)
   value=value.copy(nudge=VoiceNudge(key,apartmentId,clock.instant().plusMillis(durationMillis)))
 }
 @Synchronized override fun invalidate() { value=value.copy(revision=value.revision+1,nudge=null,selectedApartmentId=null) }
 @Synchronized override fun dismissNudge(key: Long) {
  if (value.nudge?.key == key) value=value.copy(nudge=null)
 }
}
