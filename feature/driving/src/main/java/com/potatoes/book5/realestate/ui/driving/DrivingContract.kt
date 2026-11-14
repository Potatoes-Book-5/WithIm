package com.potatoes.book5.realestate.ui.driving

import com.potatoes.book5.realestate.domain.model.VehiclePose
import com.potatoes.book5.realestate.ui.common.MapPointUi
import com.potatoes.book5.realestate.ui.common.RankItemUi
import java.time.LocalDate

data class DrivingTradeUi(val name: String, val priceManwon: Long, val contractDate: LocalDate, val distanceKm: Double)

data class DrivingTradeRankingUi(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val loading: Boolean = true,
    val rows: List<DrivingTradeUi> = emptyList(),
    val error: String? = null,
)

/** 주행 중 화면의 보기. RANK = SCR-004, MAP = SCR-009 */
enum class DrivingMode { RANK, MAP }

/** SCR-005 알림. 대장 교체와 관심 매물이 한 칸을 함께 쓰고 최신 1건만 보인다(F-006 규칙 5). */
sealed interface DrivingAlertUi {
    val key: Long
}

/** SCR-005 대장 교체 알림 */
data class LeaderAlertUi(
    override val key: Long,
    val previousName: String,
    val currentName: String,
    val priceManwon: Long,
    val distanceKm: Double,
) : DrivingAlertUi

/** SCR-005 관심 매물 상단 배너(A안, T-025) */
data class InterestAlertUi(
    override val key: Long,
    val apartmentId: String,
    val name: String,
    val priceManwon: Long,
    val areaPyeong: Int,
    val distanceKm: Double,
    val bearingDeg: Double,
    val scrap: ScrapButtonState = ScrapButtonState.IDLE,
) : DrivingAlertUi {
    val isAhead: Boolean get() = bearingDeg in -90.0..90.0
}

enum class ScrapButtonState { IDLE, SAVING, SAVED }

data class DrivingScreenState(
    val mode: DrivingMode = DrivingMode.RANK,
    val radiusKm: Int = 10,
    val ranking: List<RankItemUi> = emptyList(),
    val interestMatches: List<MapPointUi> = emptyList(),
    val others: List<MapPointUi> = emptyList(),
    /** 지도 중심이 되는 차량 위치. null이면 간이 지도로 그린다. */
    val vehicle: VehiclePose? = null,
    val alert: DrivingAlertUi? = null,
    val actualTrades: DrivingTradeRankingUi? = null,
)

sealed interface DrivingIntent {
    data object Enter : DrivingIntent
    data object Exit : DrivingIntent
    data class SelectMode(val mode: DrivingMode) : DrivingIntent
    /** 관심 매물 배너의 스크랩(한 번 탭, 주행 중 허용 여부는 T-001) */
    data class ScrapAlert(val key: Long) : DrivingIntent
}

sealed interface DrivingResult {
    data class TradesUpdated(val ranking: DrivingTradeRankingUi) : DrivingResult
    data class RankingUpdated(
        val radiusKm: Int,
        val ranking: List<RankItemUi>,
        val interestMatches: List<MapPointUi>,
        val others: List<MapPointUi>,
        val vehicle: VehiclePose? = null,
    ) : DrivingResult
    data class ModeSelected(val mode: DrivingMode) : DrivingResult
    data class AlertShown(val alert: DrivingAlertUi) : DrivingResult
    data class AlertExpired(val key: Long) : DrivingResult
    data class AlertScrapChanged(val key: Long, val scrap: ScrapButtonState) : DrivingResult
}
