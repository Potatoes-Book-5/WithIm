package com.potatoes.book5.realestate.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveDao {
    @Query("SELECT * FROM drive_region_rounds WHERE session_id = :id AND round_id = :roundId")
    suspend fun round(id: Long, roundId: String): DriveRegionRoundEntity?

    @Insert
    suspend fun insertRound(round: DriveRegionRoundEntity)

    @Query("UPDATE drive_region_rounds SET ended_at = :at WHERE session_id = :id AND ended_at IS NULL AND round_id != :except")
    suspend fun closeRounds(id: Long, at: Long, except: String = "")

    @Query("SELECT * FROM drive_region_ranks WHERE session_id = :id AND round_id = :roundId ORDER BY rank")
    suspend fun roundRanks(id: Long, roundId: String): List<DriveRegionRankEntity>

    @Query("DELETE FROM drive_region_ranks WHERE session_id = :id AND round_id = :roundId")
    suspend fun clearRoundRanks(id: Long, roundId: String)

    @Insert
    suspend fun insertRoundRanks(ranks: List<DriveRegionRankEntity>)

    @Query("SELECT * FROM drive_region_rounds WHERE session_id = :id ORDER BY started_at, round_id")
    fun observeRounds(id: Long): Flow<List<DriveRegionRoundEntity>>

    @Query("SELECT * FROM drive_region_ranks WHERE session_id = :id ORDER BY rank")
    fun observeRoundRanks(id: Long): Flow<List<DriveRegionRankEntity>>

    @Query("SELECT COUNT(*) FROM drive_region_ranks WHERE session_id = :id")
    suspend fun roundRankCount(id: Long): Int

    @Query("SELECT id FROM drive_sessions WHERE ended_at IS NULL ORDER BY started_at DESC LIMIT 1")
    suspend fun openSessionId(): Long?

    @Insert
    suspend fun insertSession(session: DriveSessionEntity): Long

    @Query("SELECT * FROM drive_sessions WHERE id = :id")
    suspend fun session(id: Long): DriveSessionEntity?

    @Query("UPDATE drive_sessions SET last_recorded_at = :at WHERE id = :id")
    suspend fun touch(id: Long, at: Long)

    @Query("DELETE FROM drive_ranks WHERE session_id = :id")
    suspend fun clearRanks(id: Long)

    @Insert
    suspend fun insertRanks(ranks: List<DriveRankEntity>)

    @Query("SELECT * FROM drive_passed_interests WHERE session_id = :id AND apartment_id = :apartmentId")
    suspend fun passed(id: Long, apartmentId: String): DrivePassedInterestEntity?

    @Upsert
    suspend fun upsertPassed(item: DrivePassedInterestEntity)

    @Insert
    suspend fun insertLeaderChange(change: DriveLeaderChangeEntity)

    @Query("SELECT COUNT(*) FROM drive_ranks WHERE session_id = :id")
    suspend fun rankCount(id: Long): Int

    @Query("UPDATE drive_sessions SET ended_at = :endedAt WHERE id = :id")
    suspend fun end(id: Long, endedAt: Long)

    /** 하위 테이블은 CASCADE로 함께 지워진다. */
    @Query("DELETE FROM drive_sessions")
    suspend fun deleteAll()

    @Query("DELETE FROM drive_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query(
        """
        DELETE FROM drive_sessions WHERE ended_at IS NOT NULL AND id NOT IN (
            SELECT id FROM drive_sessions WHERE ended_at IS NOT NULL ORDER BY ended_at DESC LIMIT :keep
        )
        """
    )
    suspend fun trim(keep: Int)

    @Query("SELECT * FROM drive_sessions WHERE id = :id AND ended_at IS NOT NULL")
    fun observeSession(id: Long): Flow<DriveSessionEntity?>

    @Query("SELECT * FROM drive_ranks WHERE session_id = :id ORDER BY rank")
    fun observeRanks(id: Long): Flow<List<DriveRankEntity>>

    @Query("SELECT * FROM drive_leader_changes WHERE session_id = :id ORDER BY at, id")
    fun observeLeaderChanges(id: Long): Flow<List<DriveLeaderChangeEntity>>

    @Query("SELECT * FROM drive_passed_interests WHERE session_id = :id ORDER BY first_seen_at, apartment_id")
    fun observePassed(id: Long): Flow<List<DrivePassedInterestEntity>>

    @Query("SELECT * FROM drive_recommendations WHERE session_id = :id AND apartment_id = :apartmentId")
    suspend fun recommendation(id: Long, apartmentId: String): DriveRecommendationEntity?

    @Upsert
    suspend fun upsertRecommendation(item: DriveRecommendationEntity)

    @Query("SELECT * FROM drive_recommendations WHERE session_id = :id ORDER BY score_percent DESC, closest_km, apartment_id")
    fun observeRecommendations(id: Long): Flow<List<DriveRecommendationEntity>>

    @Query("SELECT COUNT(*) FROM drive_recommendations WHERE session_id = :id")
    suspend fun recommendationCount(id: Long): Int

    @Query("SELECT id FROM drive_sessions WHERE ended_at IS NOT NULL ORDER BY ended_at DESC LIMIT 1")
    fun observeLatestId(): Flow<Long?>

    @Query("SELECT id FROM drive_sessions WHERE ended_at IS NOT NULL AND seen = 0 ORDER BY ended_at DESC LIMIT 1")
    fun observeLatestUnseenId(): Flow<Long?>

    @Query("UPDATE drive_sessions SET seen = 1 WHERE id = :id")
    suspend fun markSeen(id: Long)

    /** 순위를 통째로 바꾼다. */
    @Transaction
    suspend fun replaceRanks(id: Long, ranks: List<DriveRankEntity>) {
        clearRanks(id)
        insertRanks(ranks)
    }
}
