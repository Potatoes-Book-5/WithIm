package com.potatoes.book5.realestate.domain.model

import java.time.Duration
import java.time.LocalDateTime

/** 주행 중 기록한 순위 한 줄(F-009). */
data class DriveRank(val rank: Int, val apartmentId: String, val name: String, val priceManwon: Long)

/** 대장 교체 기록. 새로 대장이 된 단지와 시각 */
data class DriveLeaderChange(
    val at: LocalDateTime, val apartmentId: String, val name: String,
    val kind: LeaderEventKind = LeaderEventKind.REPLACED,
    val districtName: String? = null,
)

/** Best encountered properties in one confirmed district visit. No location coordinates. */
data class DriveRegionRound(
    val round: MarketRound,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime?,
    val top: List<DriveRank>,
)

/** 지나친 관심 매물. 가장 가까웠던 거리를 남긴다. */
data class PassedInterest(val apartmentId: String, val name: String, val closestKm: Double, val priceManwon: Long)

/** 주행 요약(SCR-008). */
data class DriveSummary(
    val id: Long,
    val startedAt: LocalDateTime,
    val endedAt: LocalDateTime,
    /** 주행 거리. 위치 SDK 연결 전에는 null(T-018 결정 로그) */
    val distanceKm: Double?,
    val radiusKm: Int,
    val finalTop: List<DriveRank>,
    val leaderChanges: List<DriveLeaderChange>,
    val passedInterests: List<PassedInterest>,
    val seen: Boolean,
    val rounds: List<DriveRegionRound> = emptyList(),
) {
    val duration: Duration get() = Duration.between(startedAt, endedAt)
    val tripTop: List<DriveRank> get() = if (rounds.isEmpty()) finalTop else rounds.flatMap { it.top }
        .groupBy { it.apartmentId }.values.map { items -> items.maxBy { it.priceManwon } }
        .sortedWith(compareByDescending<DriveRank> { it.priceManwon }.thenBy { it.apartmentId })
        .take(3).mapIndexed { index, rank -> rank.copy(rank = index + 1) }
}
