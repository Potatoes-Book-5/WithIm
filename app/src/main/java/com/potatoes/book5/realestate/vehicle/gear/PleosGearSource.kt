package com.potatoes.book5.realestate.vehicle.gear

import android.content.Context
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import com.potatoes.book5.realestate.domain.model.GearPosition
import com.potatoes.book5.realestate.domain.model.GearReading
import com.potatoes.book5.realestate.domain.port.GearStateSource
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Pleos Vehicle SDK `Gear` 도메인으로 기어 상태를 받는다(T-034, docs/platform/sdk-usage-map.md).
 *
 * - 수집하는 동안 새 [Vehicle]을 연다: initialize → getGear → getCurrentGearSelectionState, registerGearSelection.
 *   닫을 때 unregisterGearSelection → release. `release()` 뒤 같은 인스턴스는 쓰지 않는다.
 * - `Gear`에는 `checkXXXCapabilities`가 없어 지원 여부를 미리 확인하지 못한다(미결). 호출이 실패하면
 *   [GearReading.Unavailable]을 보내 도메인이 대체 흐름을 쓰게 한다. 차량 모델명으로 분기하지 않는다(CP-006).
 * - 연결 직후 현재 값 조회가 실패할 수 있어, 기어를 받기 전까지 현재 값을 몇 번 더 묻는다(HW-006).
 * - 콜백 스레드는 문서에 없어 trySend로 넘긴다.
 */
class PleosGearSource(context: Context) : GearStateSource {
    private val appContext = context.applicationContext

    override val gear: Flow<GearReading> = callbackFlow {
        val received = AtomicBoolean(false)
        // SDK가 null을 줄 수 있다고 선언한다. 기어를 모르는 값으로 다룬다.
        fun deliver(gear: VehicleGear?) {
            received.set(true)
            trySend(GearReading.Selected(gear?.toDomain() ?: GearPosition.UNKNOWN))
        }
        fun fail(where: String, error: Throwable) {
            Log.w(TAG, "Gear $where failed", error)
            trySend(GearReading.Unavailable)
        }

        val vehicle = Vehicle(appContext)
        val listener = object : GearSelectionListener {
            override fun onSelected(gear: VehicleGear?) = deliver(gear)
            override fun onFailed(exception: Exception) = fail("listener", exception)
        }
        val gearApi = runCatching {
            vehicle.initialize()
            vehicle.getGear().also { it.registerGearSelection(listener) }
        }.onFailure { fail("registration", it) }.getOrNull()

        if (gearApi != null) launch {
            repeat(CURRENT_STATE_ATTEMPTS) { attempt ->
                if (attempt > 0) delay(RETRY_DELAY_MILLIS)
                if (received.get()) return@launch
                runCatching {
                    gearApi.getCurrentGearSelectionState(::deliver) { fail("current state", it) }
                }.onFailure { fail("current state", it) }
            }
        }

        awaitClose {
            if (gearApi != null) runCatching { gearApi.unregisterGearSelection(listener) }
            runCatching { vehicle.release() }
        }
    }

    private companion object {
        const val TAG = "PleosGear"
        const val CURRENT_STATE_ATTEMPTS = 3
        const val RETRY_DELAY_MILLIS = 2_000L
    }
}

internal fun VehicleGear.toDomain(): GearPosition = when (this) {
    VehicleGear.GEAR_PARK -> GearPosition.PARK
    VehicleGear.GEAR_REVERSE -> GearPosition.REVERSE
    VehicleGear.GEAR_NEUTRAL -> GearPosition.NEUTRAL
    VehicleGear.GEAR_DRIVE -> GearPosition.DRIVE
    VehicleGear.GEAR_UNKNOWN -> GearPosition.UNKNOWN
    VehicleGear.GEAR_1, VehicleGear.GEAR_2, VehicleGear.GEAR_3, VehicleGear.GEAR_4, VehicleGear.GEAR_5,
    VehicleGear.GEAR_6, VehicleGear.GEAR_7, VehicleGear.GEAR_8, VehicleGear.GEAR_9 -> GearPosition.MANUAL
}
