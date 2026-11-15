package com.potatoes.book5.realestate.domain.model.appaction
import java.time.Instant

data class VoiceNudge(val key: Long, val apartmentId: String, val expiresAt: Instant)
data class VoiceContextSnapshot(
 val revision: Long = 0,
 val foreground: Boolean = false,
 val enabled: Boolean = false,
 val selectedApartmentId: String? = null,
 val nudge: VoiceNudge? = null,
 val capturedAt: Instant = Instant.EPOCH,
 val visualDetailsAllowed: Boolean = false,
) {
 fun apartmentId(): String? = if (!foreground || !enabled) null
 else selectedApartmentId ?: nudge?.takeIf { it.expiresAt > capturedAt }?.apartmentId
}
interface VoiceContextPort {
 val changes: kotlinx.coroutines.flow.Flow<VoiceContextSnapshot> get() = kotlinx.coroutines.flow.emptyFlow()
 fun snapshot(): VoiceContextSnapshot
 fun isCurrent(snapshot: VoiceContextSnapshot): Boolean
 fun setConsented(value: Boolean)
 fun setForeground(value: Boolean)
 fun setAppState(enabled: Boolean, selectedApartmentId: String?, nudgeVisible: Boolean = false, visualDetailsAllowed: Boolean = false)
 fun showNudge(key: Long, apartmentId: String, durationMillis: Long)
 fun invalidate()
 fun dismissNudge(key: Long)
}
