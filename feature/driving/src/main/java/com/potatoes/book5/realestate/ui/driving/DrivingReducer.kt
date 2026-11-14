package com.potatoes.book5.realestate.ui.driving

internal fun reduceDriving(state: DrivingScreenState, result: DrivingResult): DrivingScreenState = when (result) {
    is DrivingResult.TradesUpdated -> state.copy(actualTrades = result.ranking)
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
    // 배너가 이미 닫혔거나 다른 알림으로 바뀌었으면 무시한다.
    is DrivingResult.AlertScrapChanged -> when (val alert = state.alert) {
        is InterestAlertUi -> if (alert.key == result.key) state.copy(alert = alert.copy(scrap = result.scrap)) else state
        else -> state
    }
}
