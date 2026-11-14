package com.potatoes.book5.realestate.ui.driving

import com.potatoes.book5.realestate.ui.common.MapPointUi
import com.potatoes.book5.realestate.ui.common.RankItemUi

/** 주행 중 화면의 보기. RANK = SCR-004, MAP = SCR-009 */
enum class DrivingMode { RANK, MAP }

/** SCR-005 대장 교체 알림 */
data class LeaderAlertUi(
    val key: Long,
    val previousName: String,
    val currentName: String,
    val priceManwon: Long,
    val distanceKm: Double,
)

data class DrivingScreenState(
    val mode: DrivingMode = DrivingMode.RANK,
    val radiusKm: Int = 10,
    val ranking: List<RankItemUi> = emptyList(),
    val interestMatches: List<MapPointUi> = emptyList(),
    val others: List<MapPointUi> = emptyList(),
    val alert: LeaderAlertUi? = null,
)

sealed interface DrivingIntent {
    data class SelectMode(val mode: DrivingMode) : DrivingIntent
}

sealed interface DrivingResult {
    data class RankingUpdated(
        val radiusKm: Int,
        val ranking: List<RankItemUi>,
        val interestMatches: List<MapPointUi>,
        val others: List<MapPointUi>,
    ) : DrivingResult
    data class ModeSelected(val mode: DrivingMode) : DrivingResult
    data class AlertShown(val alert: LeaderAlertUi) : DrivingResult
    data class AlertExpired(val key: Long) : DrivingResult
}
