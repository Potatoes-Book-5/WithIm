package com.potatoes.book5.realestate.vehicle.gear

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.potatoes.book5.realestate.domain.model.GearPosition
import com.potatoes.book5.realestate.domain.model.GearReading
import com.potatoes.book5.realestate.domain.port.GearStateSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * AAOS `CarPropertyManager`의 `GEAR_SELECTION`으로 기어 상태를 받는다(T-034, API-002, CP-005, docs/platform/sdk-usage-map.md).
 *
 * - Pleos Vehicle SDK 2.3.2의 `Gear` API는 이 속성을 쓰라며 deprecated 되어 이 어댑터로 바꿨다.
 * - 읽기 권한은 normal 보호 수준의 `android.car.permission.CAR_POWERTRAIN`이다.
 * - 지원 여부는 `getCarPropertyConfig(GEAR_SELECTION)`로 확인한다(CP-005). 설정이 없거나 지원 기어(configArray)에 P가 없으면
 *   주행 완료를 정할 수 없어 [GearReading.Unavailable]을 보낸다. 차량 모델명으로 분기하지 않는다(CP-006).
 * - on-change 속성은 등록할 때 현재 값을 첫 이벤트로 준다. `Car.createCar`의 준비 콜백은 차량 서비스가 다시 시작되면 다시 오므로
 *   준비될 때마다 콜백을 다시 등록한다.
 */
class CarGearSource(context: Context) : GearStateSource {
    private val appContext = context.applicationContext

    override val gear: Flow<GearReading> = callbackFlow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
            !appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        ) {
            trySend(GearReading.Unavailable)
            awaitClose()
            return@callbackFlow
        }

        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                if (value.propertyId != VehiclePropertyIds.GEAR_SELECTION) return
                Log.d(TAG, "GEAR_SELECTION value=${value.value} status=${value.status}")
                val gear = (value.value as? Int)
                    ?.takeIf { value.status == CarPropertyValue.STATUS_AVAILABLE }
                    ?.let(::gearPositionOf)
                    ?: GearPosition.UNKNOWN
                trySend(GearReading.Selected(gear))
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                Log.w(TAG, "GEAR_SELECTION error event")
                trySend(GearReading.Unavailable)
            }
        }

        var manager: CarPropertyManager? = null
        fun detach() {
            manager?.let { runCatching { it.unregisterCallback(callback) } }
            manager = null
        }

        val car = runCatching {
            Car.createCar(appContext, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { car, ready ->
                detach()
                val properties = if (ready) car.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager else null
                val config = properties?.let { runCatching { it.getCarPropertyConfig(VehiclePropertyIds.GEAR_SELECTION) }.getOrNull() }
                if (properties == null || config == null || VehicleGear.GEAR_PARK !in config.configArray) {
                    Log.w(TAG, "GEAR_SELECTION unavailable: ready=$ready manager=${properties != null} config=${config?.configArray}")
                    trySend(GearReading.Unavailable)
                    return@createCar
                }
                val registered = runCatching {
                    properties.registerCallback(callback, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
                }.onFailure { Log.w(TAG, "GEAR_SELECTION registration failed", it) }.getOrDefault(false)
                if (registered) manager = properties else trySend(GearReading.Unavailable)
            }
        }.onFailure {
            Log.w(TAG, "Car service connection failed", it)
            trySend(GearReading.Unavailable)
        }.getOrNull()

        awaitClose {
            detach()
            runCatching { car?.disconnect() }
        }
    }

    private companion object {
        const val TAG = "CarGear"
    }
}

/** AAOS `VehicleGear` 값을 도메인 기어로 바꾼다. 1~9단은 수동 단수다. */
internal fun gearPositionOf(value: Int): GearPosition = when (value) {
    VehicleGear.GEAR_PARK -> GearPosition.PARK
    VehicleGear.GEAR_REVERSE -> GearPosition.REVERSE
    VehicleGear.GEAR_NEUTRAL -> GearPosition.NEUTRAL
    VehicleGear.GEAR_DRIVE -> GearPosition.DRIVE
    VehicleGear.GEAR_FIRST, VehicleGear.GEAR_SECOND, VehicleGear.GEAR_THIRD, VehicleGear.GEAR_FOURTH, VehicleGear.GEAR_FIFTH,
    VehicleGear.GEAR_SIXTH, VehicleGear.GEAR_SEVENTH, VehicleGear.GEAR_EIGHTH, VehicleGear.GEAR_NINTH -> GearPosition.MANUAL
    else -> GearPosition.UNKNOWN
}
