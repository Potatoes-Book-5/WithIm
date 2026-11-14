package com.potatoes.book5.realestate.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrapDao {
    @Query("SELECT * FROM scraps ORDER BY scrapped_at DESC")
    fun observeAll(): Flow<List<ScrapEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM scraps WHERE apartment_id = :apartmentId)")
    fun observeExists(apartmentId: String): Flow<Boolean>

    @Query("SELECT * FROM scraps ORDER BY scrapped_at DESC")
    suspend fun getAll(): List<ScrapEntity>

    @Query("SELECT * FROM scraps WHERE apartment_id = :apartmentId")
    suspend fun get(apartmentId: String): ScrapEntity?

    @Upsert
    suspend fun upsert(entity: ScrapEntity)

    @Query("DELETE FROM scraps WHERE apartment_id = :apartmentId")
    suspend fun delete(apartmentId: String)

    @Query("UPDATE scraps SET change_seen = 1 WHERE apartment_id = :apartmentId AND change_kind IS NOT NULL")
    suspend fun markChangeSeen(apartmentId: String)
}
