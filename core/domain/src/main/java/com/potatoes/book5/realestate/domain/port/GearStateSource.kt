package com.potatoes.book5.realestate.domain.port

import com.potatoes.book5.realestate.domain.model.GearReading
import kotlinx.coroutines.flow.Flow

/**
 * 차량 기어 상태(T-034). 구현은 vehicle/ 패키지의 AAOS `GEAR_SELECTION` 어댑터다.
 * 수집하는 동안 SDK에 연결하고, 수집을 멈추면 해제한다.
 */
interface GearStateSource {
    val gear: Flow<GearReading>
}
