package com.potatoes.book5.realestate.domain.model

/** 차량 기어 위치(T-034). SDK 값은 vehicle/ 패키지에서 이 값으로 바꾼다. */
enum class GearPosition {
    PARK,
    REVERSE,
    NEUTRAL,
    DRIVE,

    /** 수동 단수(1~9단) */
    MANUAL,

    /** SDK가 기어를 알 수 없다고 알린 값. 주행 상태를 바꾸지 않는다. */
    UNKNOWN,
}

/** 기어 상태 수신 결과 */
sealed interface GearReading {
    data class Selected(val gear: GearPosition) : GearReading

    /** 기어를 읽지 못했다. 서비스 미설치, 권한 부족, 연결 실패, 미지원을 SDK 오류만으로 구분할 수 없어 하나로 둔다. */
    data object Unavailable : GearReading
}
