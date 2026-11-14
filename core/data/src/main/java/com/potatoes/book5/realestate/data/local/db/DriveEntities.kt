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
