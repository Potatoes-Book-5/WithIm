package com.potatoes.book5.realestate.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DriveDao {
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
