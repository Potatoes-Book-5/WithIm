package com.potatoes.book5.realestate.vehicle.simulation

import com.potatoes.book5.realestate.domain.model.DrivingState
import com.potatoes.book5.realestate.domain.port.DrivingStateProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * CarUxRestrictions 연동(DI-001) 전까지 쓰는 주행 상태. 처음에는 주차 상태이며
 * 개발 빌드의 버튼으로만 바꿀 수 있다.
 */
class SimulatedDrivingStateProvider : DrivingStateProvider {
    private val state = MutableStateFlow(DrivingState.PARKED)
    override val drivingState: Flow<DrivingState> = state

    fun setDriving(driving: Boolean) {
        state.value = if (driving) DrivingState.DRIVING else DrivingState.PARKED
    }
}
