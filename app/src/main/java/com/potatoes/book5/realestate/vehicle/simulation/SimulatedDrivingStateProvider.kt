package com.potatoes.book5.realestate.vehicle.simulation

import com.potatoes.book5.realestate.domain.model.DrivingState
import com.potatoes.book5.realestate.domain.port.DrivingStateProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 개발 빌드의 수동 주행 전환. 처음에는 주차 상태이며 개발 빌드의 버튼으로만 바꿀 수 있다.
 * Pleos 기어를 읽지 못하는 환경에서는 `GearDrivingStateProvider`가 이 값을 대신 쓴다(T-034).
 */
class SimulatedDrivingStateProvider : DrivingStateProvider {
    private val state = MutableStateFlow(DrivingState.PARKED)
    override val drivingState: Flow<DrivingState> = state

    fun setDriving(driving: Boolean) {
        state.value = if (driving) DrivingState.DRIVING else DrivingState.PARKED
    }
}
