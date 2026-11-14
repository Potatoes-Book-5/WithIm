package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.model.VehiclePose
import java.time.Instant
import kotlinx.coroutines.flow.Flow

enum class LocationStatus { SEARCHING, AVAILABLE, CONSENT_REQUIRED, PERMISSION_REQUIRED, UNSUPPORTED, DISABLED, STALE, INACCURATE }
enum class LocationSource { VEHICLE, EMULATOR_ROUTE }

data class VehicleLocationState(
    val pose: VehiclePose? = null,
    val accuracyMeters: Double? = null,
    val observedAt: Instant? = null,
    val status: LocationStatus = LocationStatus.SEARCHING,
    val source: LocationSource = LocationSource.VEHICLE,
)

/** Continuous fixes from a vehicle or emulator location adapter. */
interface VehicleLocationProvider {
    val locations: Flow<VehicleLocationState>
}
