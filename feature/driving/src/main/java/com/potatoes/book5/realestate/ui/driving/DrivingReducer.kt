package com.potatoes.book5.realestate.ui.driving

internal fun reduceDriving(state: DrivingScreenState, result: DrivingResult): DrivingScreenState = when (result) {
    is DrivingResult.RankingUpdated -> state.copy(
        radiusKm = result.radiusKm,
        ranking = result.ranking,
        interestMatches = result.interestMatches,
        others = result.others,
        vehicle = result.vehicle,
    )
    is DrivingResult.ModeSelected -> state.copy(mode = result.mode)
    // 알림이 겹치면 최신 1건만 보여 준다(F-006 규칙 5).
    is DrivingResult.AlertShown -> state.copy(alert = result.alert)
    // 더 새로운 알림이 떠 있으면 이전 알림의 만료로 지우지 않는다.
    is DrivingResult.AlertExpired -> if (state.alert?.key == result.key) state.copy(alert = null) else state
}
