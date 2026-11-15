package com.potatoes.book5.realestate.data.local.db

import androidx.room.*

@Entity(tableName="tour_areas", primaryKeys=["profile","area_id"])
data class TourAreaEntity(
    val profile: String, @ColumnInfo(name="area_id") val areaId: String, val name: String,
    @ColumnInfo(name="district_code") val districtCode: String, val shape: String,
    val latitude: Double?, val longitude: Double?, @ColumnInfo(name="radius_meters") val radiusMeters: Double?,
    @ColumnInfo(name="source_id") val sourceId: String, @ColumnInfo(name="source_as_of") val sourceAsOf: Long,
    @ColumnInfo(name="saved_at") val savedAt: Long, val generation: String, @ColumnInfo(name="district_name") val districtName: String? = null,
)
@Entity(tableName="tour_briefings", primaryKeys=["profile","area_id"],
    foreignKeys=[ForeignKey(entity=TourAreaEntity::class,parentColumns=["profile","area_id"],childColumns=["profile","area_id"],onDelete=ForeignKey.CASCADE)])
data class TourBriefingEntity(
    val profile: String, @ColumnInfo(name="area_id") val areaId: String, val version: String,
    @ColumnInfo(name="average_krw") val averageKrw: Long?, @ColumnInfo(name="highest_krw") val highestKrw: Long?,
    @ColumnInfo(name="sample_count") val sampleCount: Int, @ColumnInfo(name="period_start") val periodStart: Long,
    @ColumnInfo(name="period_end") val periodEnd: Long, @ColumnInfo(name="source_as_of") val sourceAsOf: Long,
    @ColumnInfo(name="observed_at") val observedAt: Long, @ColumnInfo(name="read_version") val readVersion: String?,
)
@Entity(tableName="tour_entries", primaryKeys=["profile","area_id"],
    foreignKeys=[ForeignKey(entity=TourAreaEntity::class,parentColumns=["profile","area_id"],childColumns=["profile","area_id"],onDelete=ForeignKey.CASCADE)])
data class TourEntryEntity(val profile: String,@ColumnInfo(name="area_id") val areaId: String,val session: String,val visit: Long,val at: Long,@ColumnInfo(name="context_revision") val contextRevision: Long,@ColumnInfo(name="area_generation") val areaGeneration: String)

@Entity(tableName="tour_notices",primaryKeys=["profile","area_id","notice_id"],
    foreignKeys=[ForeignKey(entity=TourAreaEntity::class,parentColumns=["profile","area_id"],childColumns=["profile","area_id"],onDelete=ForeignKey.CASCADE)],
    indices=[Index(value=["profile","notice_id"])])
data class TourNoticeEntity(val profile:String,@ColumnInfo(name="area_id") val areaId:String,@ColumnInfo(name="notice_id") val noticeId:String,
    val source:String,val type:String,val title:String,val url:String,@ColumnInfo(name="published_at") val publishedAt:Long?,@ColumnInfo(name="published_date") val publishedDate:Long?,val address:String?,val scope:String,
    val version:String,@ColumnInfo(name="collected_at") val collectedAt:Long,@ColumnInfo(name="observed_at") val observedAt:Long,@ColumnInfo(name="read_version") val readVersion:String?)
@Entity(tableName="tour_sources",primaryKeys=["profile","source"])
data class TourSourceEntity(val profile:String,val source:String,val state:String,@ColumnInfo(name="attempted_at") val attemptedAt:Long)
