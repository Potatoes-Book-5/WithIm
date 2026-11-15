package com.potatoes.book5.realestate.data.local.db
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TourDao {
    @Query("SELECT * FROM tour_areas WHERE profile=:profile ORDER BY saved_at DESC, area_id")
    suspend fun areas(profile: String): List<TourAreaEntity>
    @Query("SELECT * FROM tour_areas WHERE profile=:profile ORDER BY saved_at DESC, area_id")
    fun observeAreas(profile: String): Flow<List<TourAreaEntity>>
    @Query("SELECT * FROM tour_areas WHERE profile=:profile AND area_id=:id")
    suspend fun area(profile: String,id: String): TourAreaEntity?
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun insertArea(value: TourAreaEntity): Long
    @Query("DELETE FROM tour_areas WHERE profile=:profile AND area_id=:id") suspend fun deleteArea(profile: String,id: String)
    @Query("DELETE FROM tour_areas") suspend fun deleteAll()
    @Query("SELECT * FROM tour_briefings WHERE profile=:profile AND area_id=:id") suspend fun summary(profile: String,id: String): TourBriefingEntity?
    @Upsert suspend fun upsertSummary(value: TourBriefingEntity)
    @Query("SELECT * FROM tour_briefings WHERE profile=:profile AND (read_version IS NULL OR read_version != version) ORDER BY observed_at DESC, area_id LIMIT :limit")
    suspend fun unread(profile: String,limit: Int): List<TourBriefingEntity>
    @Query("UPDATE tour_briefings SET read_version=:version WHERE profile=:profile AND area_id=:id AND version=:version AND (read_version IS NULL OR read_version != :version)")
    suspend fun markRead(profile: String,id: String,version: String): Int
    @Query("SELECT * FROM tour_entries WHERE profile=:profile AND area_id=:areaId ORDER BY at DESC LIMIT 1")
    suspend fun latestEntry(profile: String,areaId: String): TourEntryEntity?
    @Upsert suspend fun insertEntry(value: TourEntryEntity)
    @Query("SELECT * FROM tour_notices WHERE profile=:profile AND notice_id=:id ORDER BY observed_at DESC LIMIT 1")
    suspend fun notice(profile:String,id:String): TourNoticeEntity?
    @Query("DELETE FROM tour_notices WHERE profile=:profile AND notice_id=:id") suspend fun removeNotice(profile:String,id:String)
    @Upsert suspend fun putNotice(value:TourNoticeEntity)
    @Query("SELECT * FROM tour_notices WHERE profile=:profile AND (read_version IS NULL OR read_version!=version) GROUP BY notice_id,version ORDER BY observed_at DESC,notice_id LIMIT 30")
    suspend fun unreadNotices(profile:String): List<TourNoticeEntity>
    @Query("UPDATE tour_notices SET read_version=:version WHERE profile=:profile AND notice_id=:id AND version=:version AND (read_version IS NULL OR read_version!=:version)")
    suspend fun markNoticeRead(profile:String,id:String,version:String): Int
    @Query("DELETE FROM tour_notices WHERE profile=:profile AND area_id=:areaId AND notice_id NOT IN (SELECT notice_id FROM tour_notices WHERE profile=:profile AND area_id=:areaId ORDER BY observed_at DESC,notice_id LIMIT 20)")
    suspend fun trimNotices(profile:String,areaId:String)
    @Upsert suspend fun putSource(value:TourSourceEntity)
    @Query("SELECT * FROM tour_sources WHERE profile=:profile") suspend fun sources(profile:String): List<TourSourceEntity>
    @Query("DELETE FROM tour_sources") suspend fun deleteSources()
}
