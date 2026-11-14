package com.potatoes.book5.realestate.vehicle.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.potatoes.book5.realestate.RealEstateApplication
import com.potatoes.book5.realestate.domain.model.GeoPoint
import com.potatoes.book5.realestate.domain.model.VehiclePose
import com.potatoes.book5.realestate.domain.port.LocationSource
import java.time.Instant

/** Debug APK only: inject a route through the same input used by future Pleos callbacks. */
class DebugLocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.potatoes.book5.realestate.DEBUG_LOCATION" ||
            !intent.hasExtra("latitude") || !intent.hasExtra("longitude") || !intent.hasExtra("accuracy")) return
        val latitude = intent.getStringExtra("latitude")?.toDoubleOrNull() ?: return
        val longitude = intent.getStringExtra("longitude")?.toDoubleOrNull() ?: return
        val accuracy = intent.getStringExtra("accuracy")?.toDoubleOrNull() ?: return
        val heading = intent.getStringExtra("heading")?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..360.0 }
        (context.applicationContext as RealEstateApplication).container.vehicleLocationProvider.updateLocation(
            VehiclePose(GeoPoint(latitude, longitude), heading), accuracy, Instant.now(), LocationSource.EMULATOR_ROUTE,
        )
    }
}
