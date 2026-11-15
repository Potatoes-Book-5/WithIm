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

/** 추천 이유를 나중에 문구로 바꿀 수 있도록 축 키를 보관한다. */
enum class RecommendationKey { PRICE, AREA, NEIGHBORHOOD, HOUSEHOLDS, NEWNESS }

data class DriveRecommendation(
    val apartmentId: String,
    val name: String,
    val priceManwon: Long,
    val areaPyeong: Int,
    val scorePercent: Int,
    val closestKm: Double,
    val mainRecoKey: RecommendationKey,
    val neighborhoodName: String? = null,
    val households: Int? = null,
    val builtYear: Int? = null,
)

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
    /** 주행 중 평가된 추천 후보. 표시 계층은 상위 3개를 사용한다. */
    val recommendations: List<DriveRecommendation> = emptyList(),
) {
    val duration: Duration get() = Duration.between(startedAt, endedAt)
    val tripTop: List<DriveRank> get() = if (rounds.isEmpty()) finalTop else rounds.flatMap { it.top }
        .groupBy { it.apartmentId }.values.map { items -> items.maxBy { it.priceManwon } }
        .sortedWith(compareByDescending<DriveRank> { it.priceManwon }.thenBy { it.apartmentId })
        .take(3).mapIndexed { index, rank -> rank.copy(rank = index + 1) }
    val recommendationTop: List<DriveRecommendation> get() = recommendations.take(3)
    /** Passed items already represented by featured recommendation cards are not repeated. */
    val additionalPassedInterests: List<PassedInterest>
        get() {
            val featuredIds = recommendationTop.mapTo(HashSet()) { it.apartmentId }
            return passedInterests.filterNot { it.apartmentId in featuredIds }
        }
    val interestSummaryCount: Int get() = recommendationTop.size + additionalPassedInterests.size
}
