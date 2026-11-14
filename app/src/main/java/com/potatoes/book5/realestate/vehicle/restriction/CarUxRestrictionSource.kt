package com.potatoes.book5.realestate.vehicle.restriction

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.potatoes.book5.realestate.domain.port.UxRestrictionSource
import com.potatoes.book5.realestate.domain.port.UxRestrictionState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * AAOS `CarUxRestrictionsManager`로 주행 규제 상태를 받는다(T-035, DI-001, docs/platform/driving-restriction-impl.md).
 *
 * - 차량 환경인지 먼저 확인한다(HW-003). 차량이 아니거나 연결하지 못하면 [UxRestrictionState.UNAVAILABLE]이다.
 * - `Car.createCar(context, handler, waitTimeout, listener)`는 차량 서비스가 다시 시작되면 다시 준비 콜백을 준다.
 *   준비될 때마다 현재 값을 보내고 리스너를 등록한다.
 * - 앱은 `isRequiresDistractionOptimization()`만 쓴다. 규제가 켜지면 DO 화면(주행 화면)만 보여 주고 나머지 기능을 막는다(DI-002).
 */
class CarUxRestrictionSource(context: Context) : UxRestrictionSource {
    private val appContext = context.applicationContext

    override val state: Flow<UxRestrictionState> = callbackFlow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            !appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        ) {
            trySend(UxRestrictionState.UNAVAILABLE)
            awaitClose()
            return@callbackFlow
        }

        var manager: CarUxRestrictionsManager? = null
        fun detach() {
            manager?.let { runCatching { it.unregisterListener() } }
            manager = null
        }

        val car = runCatching {
            Car.createCar(appContext, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { car, ready ->
                detach()
                val restrictions = if (ready) car.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager else null
                if (restrictions == null) {
                    trySend(UxRestrictionState.UNAVAILABLE)
                    return@createCar
                }
                manager = restrictions
                trySend(restrictions.currentCarUxRestrictions.toState())
                restrictions.registerListener { trySend(it.toState()) }
            }
        }.onFailure {
            Log.w(TAG, "Car service connection failed", it)
            trySend(UxRestrictionState.UNAVAILABLE)
        }.getOrNull()

        awaitClose {
            detach()
            runCatching { car?.disconnect() }
        }
    }

    private fun CarUxRestrictions?.toState(): UxRestrictionState = when {
        this == null -> UxRestrictionState.UNAVAILABLE
        isRequiresDistractionOptimization -> UxRestrictionState.RESTRICTED
        else -> UxRestrictionState.UNRESTRICTED
    }

    private companion object {
        const val TAG = "CarUxRestriction"
    }
}
