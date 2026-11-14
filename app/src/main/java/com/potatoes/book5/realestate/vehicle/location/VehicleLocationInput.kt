package com.potatoes.book5.realestate.vehicle.location

import com.potatoes.book5.realestate.domain.model.VehiclePose
import com.potatoes.book5.realestate.domain.port.ConsentRepository
import com.potatoes.book5.realestate.domain.port.LocationSource
import com.potatoes.book5.realestate.domain.port.LocationStatus
import com.potatoes.book5.realestate.domain.port.VehicleLocationProvider
import com.potatoes.book5.realestate.domain.port.VehicleLocationState
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/** Pleos location callbacks feed this input; this class does not collect GPS itself. */
class VehicleLocationInput(private val consentRepository: ConsentRepository) : VehicleLocationProvider {
    private val incoming = MutableStateFlow(VehicleLocationState())

    fun updateLocation(
        pose: VehiclePose,
        /** 미터. SDK가 단위를 문서로 제공하지 않으면 null(미제공)이고 정확도 기준을 적용하지 않는다(T-029). */
        accuracyMeters: Double?,
        observedAt: Instant,
        source: LocationSource = LocationSource.VEHICLE,
    ) {
        val valid = pose.location.latitude.isFinite() && pose.location.latitude in -90.0..90.0 &&
            pose.location.longitude.isFinite() && pose.location.longitude in -180.0..180.0 &&
            (accuracyMeters == null || (accuracyMeters.isFinite() && accuracyMeters in 0.0..100.0))
        incoming.value = VehicleLocationState(
            pose = pose.takeIf { valid }?.copy(headingDeg = pose.headingDeg?.takeIf {
                it.isFinite() && it in 0.0..360.0
            }?.rem(360.0)), accuracyMeters = accuracyMeters, observedAt = observedAt,
            status = if (valid) LocationStatus.AVAILABLE else LocationStatus.INACCURATE, source = source,
        )
    }

    fun unavailable(status: LocationStatus) {
        require(status != LocationStatus.AVAILABLE)
        incoming.value = VehicleLocationState(status = status)
    }

    override val locations: Flow<VehicleLocationState> = combine(
        incoming, consentRepository.observe(), flow {
            while (true) { emit(Instant.now()); delay(5_000) }
        },
    ) { fix, consent, _ ->
        val now = Instant.now()
        when {
            consent?.locationConsented != true -> VehicleLocationState(status = LocationStatus.CONSENT_REQUIRED)
            fix.status == LocationStatus.AVAILABLE && (fix.observedAt == null ||
                Duration.between(fix.observedAt, now).let { it.isNegative || it > Duration.ofSeconds(30) }) ->
                fix.copy(pose = null, status = LocationStatus.STALE)
            else -> fix
        }
    }.distinctUntilChanged()
}
