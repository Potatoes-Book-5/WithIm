package com.potatoes.book5.realestate.data.repository

import com.potatoes.book5.realestate.domain.model.appaction.PriceLocationSnapshot
import com.potatoes.book5.realestate.domain.model.appaction.ResolvedArea
import com.potatoes.book5.realestate.domain.policy.GeoMath
import com.potatoes.book5.realestate.domain.port.*
import java.util.concurrent.atomic.AtomicReference
import java.time.Clock

/** Background POINT lookup provenance, separate from the ranking round's 30-second dwell. */
class InMemoryPriceLocation(private val clock: Clock = Clock.systemUTC()) {
    private data class Verified(val fix: VehicleLocationState, val district: MarketDistrict)
    private data class State(val revision: Long = 0, val current: VehicleLocationState = VehicleLocationState(), val verified: Verified? = null)
    private val state = AtomicReference(State())

    fun updateFix(fix: VehicleLocationState) {
        state.updateAndGet { old ->
            val interrupted = old.current.source != fix.source ||
                (old.current.status == LocationStatus.AVAILABLE) != (fix.status == LocationStatus.AVAILABLE)
            State(old.revision + if (interrupted) 1 else 0, fix,
                old.verified.takeIf { !interrupted && fix.status == LocationStatus.AVAILABLE })
        }
    }

    fun revision(): Long = state.get().revision

    fun publish(fix: VehicleLocationState, district: MarketDistrict, expectedRevision: Long) {
        val observedAt = fix.observedAt ?: return
        if (fix.status != LocationStatus.AVAILABLE || fix.pose == null ||
            observedAt > clock.instant() || !district.code.matches(Regex("[0-9]{5}")) || district.name.isBlank()) return
        state.updateAndGet { old ->
            if (old.revision != expectedRevision || old.current.status != LocationStatus.AVAILABLE || old.current.source != fix.source ||
                old.verified?.fix?.observedAt?.let { it > observedAt } == true) old
            else old.copy(verified = Verified(fix, district))
        }
    }

    fun snapshot(): PriceLocationSnapshot {
        val current = state.get()
        val verified = current.verified
        val latestPoint = current.current.pose?.location
        // Never extrapolate an old verified point after travelling beyond the product error bound.
        val valid = verified != null && latestPoint != null && current.current.status == LocationStatus.AVAILABLE &&
            GeoMath.distanceKm(latestPoint, verified.fix.pose!!.location) <= 0.05
        return PriceLocationSnapshot(current.revision,
            if (valid) ResolvedArea(verified!!.district.code, verified.district.name) else null,
            verified?.fix?.observedAt, verified?.fix?.accuracyMeters, verified?.fix?.source)
    }

    fun isCurrent(snapshot: PriceLocationSnapshot): Boolean = state.get().revision == snapshot.revision
}
