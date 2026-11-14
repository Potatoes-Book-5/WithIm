package com.potatoes.book5.realestate.domain.model

import java.time.Duration
import java.time.LocalDateTime

/** 주행 중 기록한 순위 한 줄(F-009). */
data class DriveRank(val rank: Int, val apartmentId: String, val name: String, val priceManwon: Long)

/** 대장 교체 기록. 새로 대장이 된 단지와 시각 */
data class DriveLeaderChange(val at: LocalDateTime, val apartmentId: String, val name: String)

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
) {
    val duration: Duration get() = Duration.between(startedAt, endedAt)
}
