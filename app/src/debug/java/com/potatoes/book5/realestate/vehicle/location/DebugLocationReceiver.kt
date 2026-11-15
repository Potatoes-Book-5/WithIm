package com.potatoes.book5.realestate.vehicle.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.potatoes.book5.realestate.RealEstateApplication
import com.potatoes.book5.realestate.domain.model.GeoPoint
import com.potatoes.book5.realestate.domain.model.VehiclePose
import com.potatoes.book5.realestate.domain.port.LocationSource
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Debug APK only: inject a route through the same input used by future Pleos callbacks. */
class DebugLocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.potatoes.book5.realestate.DEBUG_LOCATION") return
        val container = (context.applicationContext as RealEstateApplication).container
        val recommendation = intent.getBooleanExtra("approachRecommendation", false)
        if (intent.getBooleanExtra("approachLeader", false) || recommendation) {
            val name = intent.getStringExtra("apartmentName")
            val arriveNow = intent.getBooleanExtra("arriveNow", false)
            routeJob?.cancel()
            routeJob = routeScope.launch {
                val condition = if (recommendation) container.observeInterestCondition().first() else null
                val scraps = if (recommendation) container.observeScraps().first() else emptyList()
                val feedback = if (recommendation) {
                    val database = com.potatoes.book5.realestate.data.local.db.AppDatabase.create(context)
                    try {
                        com.potatoes.book5.realestate.data.local.RoomRecommendationFeedbackRepository(database.recommendationFeedbackDao()).getAll()
                    } finally { database.close() }
                } else emptyList()
                fun candidates(state: com.potatoes.book5.realestate.domain.model.RankingSnapshot): List<com.potatoes.book5.realestate.domain.model.NearbyApartment> {
                    if (!recommendation) return state.top
                    val preference = condition ?: return emptyList()
                    val ids = com.potatoes.book5.realestate.domain.policy.DriveRecommendationPolicy.rank(
                        preference, state.recommendationCandidates, java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).year, scraps, feedback,
                    ).take(3).map { it.apartmentId }
                    return ids.filter { id -> scraps.none { it.apartmentId == id } }.mapNotNull { id ->
                        state.recommendationCandidates.firstOrNull { it.apartment.id == id }
                    }
                }
                val snapshot = withTimeoutOrNull(15_000) {
                    container.marketRepository.observeNearbyRanking().first { state ->
                        !state.loading && state.error == null && candidates(state).any {
                            (name == null || it.apartment.name == name) && it.apartment.location != null
                        }
                    }
                } ?: return@launch
                val start = (snapshot.vehicle ?: snapshot.mapCenter)?.location ?: return@launch
                val target = candidates(snapshot).first { name == null || it.apartment.name == name }.apartment.location
                    ?: return@launch
                // Actual API geocode, synthetic straight-line motion; coordinates stay in memory.
                android.util.Log.d("DrivingMapDemo", "Approach started")
                repeat(if (arriveNow) 1 else 61) { step ->
                    val fraction = if (arriveNow) 1.0 else step / 60.0
                    container.vehicleLocationProvider.updateLocation(
                        VehiclePose(GeoPoint(
                            start.latitude + (target.latitude - start.latitude) * fraction,
                            start.longitude + (target.longitude - start.longitude) * fraction,
                        )), 10.0, Instant.now(), LocationSource.EMULATOR_ROUTE,
                    )
                    delay(1_000)
                }
                android.util.Log.d("DrivingMapDemo", "Arrived at selected apartment")
                // Keep the endpoint visible for the user's review.
                repeat(90) {
                    container.vehicleLocationProvider.updateLocation(
                        VehiclePose(target), 10.0, Instant.now(), LocationSource.EMULATOR_ROUTE)
                    delay(1_000)
                }
            }
            return
        }
        if (!intent.hasExtra("latitude") || !intent.hasExtra("longitude") || !intent.hasExtra("accuracy")) return
        val latitude = intent.getStringExtra("latitude")?.toDoubleOrNull() ?: return
        val longitude = intent.getStringExtra("longitude")?.toDoubleOrNull() ?: return
        val accuracy = intent.getStringExtra("accuracy")?.toDoubleOrNull() ?: return
        val heading = intent.getStringExtra("heading")?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..360.0 }
        routeJob?.cancel()
        container.vehicleLocationProvider.updateLocation(
            VehiclePose(GeoPoint(latitude, longitude), heading), accuracy, Instant.now(), LocationSource.EMULATOR_ROUTE,
        )
    }

    private companion object {
        val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var routeJob: Job? = null
    }
}
