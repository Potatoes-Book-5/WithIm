package com.potatoes.book5.realestate.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 주행 세션(F-009, T-018). 시각은 epoch milli. */
@Entity(tableName = "drive_sessions")
data class DriveSessionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    /** 닫히지 않았으면 null */
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "last_recorded_at") val lastRecordedAt: Long,
    @ColumnInfo(name = "radius_km") val radiusKm: Int,
    /** 위치 SDK 연결 전에는 null */
    @ColumnInfo(name = "distance_km") val distanceKm: Double?,
    @ColumnInfo(name = "seen") val seen: Boolean,
)

@Entity(
    tableName = "drive_ranks",
    primaryKeys = ["session_id", "rank"],
    foreignKeys = [ForeignKey(entity = DriveSessionEntity::class, parentColumns = ["id"], childColumns = ["session_id"], onDelete = ForeignKey.CASCADE)],
)
data class DriveRankEntity(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "rank") val rank: Int,
    @ColumnInfo(name = "apartment_id") val apartmentId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price_manwon") val priceManwon: Long,
)

@Entity(
    tableName = "drive_leader_changes",
    foreignKeys = [ForeignKey(entity = DriveSessionEntity::class, parentColumns = ["id"], childColumns = ["session_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("session_id")],
)
data class DriveLeaderChangeEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "at") val at: Long,
    @ColumnInfo(name = "apartment_id") val apartmentId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "kind", defaultValue = "'REPLACED'") val kind: String = "REPLACED",
    @ColumnInfo(name = "district_name") val districtName: String? = null,
)

@Entity(
    tableName = "drive_passed_interests",
    primaryKeys = ["session_id", "apartment_id"],
    foreignKeys = [ForeignKey(entity = DriveSessionEntity::class, parentColumns = ["id"], childColumns = ["session_id"], onDelete = ForeignKey.CASCADE)],
)
data class DrivePassedInterestEntity(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "apartment_id") val apartmentId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "closest_km") val closestKm: Double,
    @ColumnInfo(name = "price_manwon") val priceManwon: Long,
    /** 처음 지나친 시각. 목록 정렬 기준 */
    @ColumnInfo(name = "first_seen_at") val firstSeenAt: Long,
)

@Entity(
    tableName = "drive_recommendations",
    primaryKeys = ["session_id", "apartment_id"],
    foreignKeys = [ForeignKey(entity = DriveSessionEntity::class, parentColumns = ["id"], childColumns = ["session_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["session_id", "score_percent", "closest_km"])],
)
data class DriveRecommendationEntity(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "apartment_id") val apartmentId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price_manwon") val priceManwon: Long,
    @ColumnInfo(name = "area_pyeong") val areaPyeong: Int,
    @ColumnInfo(name = "score_percent") val scorePercent: Int,
    @ColumnInfo(name = "closest_km") val closestKm: Double,
    /** Stable axis identifier; user-facing keyword mapping is a later presentation rule. */
    @ColumnInfo(name = "main_reco_key") val mainRecoKey: String,
    @ColumnInfo(name = "neighborhood_name") val neighborhoodName: String?,
    @ColumnInfo(name = "households") val households: Int?,
    @ColumnInfo(name = "built_year") val builtYear: Int?,
)

@Entity(
    tableName = "drive_region_rounds",
    primaryKeys = ["session_id", "round_id"],
    foreignKeys = [ForeignKey(entity = DriveSessionEntity::class, parentColumns = ["id"],
        childColumns = ["session_id"], onDelete = ForeignKey.CASCADE)],
)
data class DriveRegionRoundEntity(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "round_id") val roundId: String,
    @ColumnInfo(name = "district_code") val districtCode: String,
    @ColumnInfo(name = "district_name") val districtName: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
)

@Entity(
    tableName = "drive_region_ranks",
    primaryKeys = ["session_id", "round_id", "rank"],
    foreignKeys = [ForeignKey(entity = DriveRegionRoundEntity::class,
        parentColumns = ["session_id", "round_id"], childColumns = ["session_id", "round_id"],
        onDelete = ForeignKey.CASCADE)],
)
data class DriveRegionRankEntity(
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "round_id") val roundId: String,
    @ColumnInfo(name = "rank") val rank: Int,
    @ColumnInfo(name = "apartment_id") val apartmentId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "price_manwon") val priceManwon: Long,
)
