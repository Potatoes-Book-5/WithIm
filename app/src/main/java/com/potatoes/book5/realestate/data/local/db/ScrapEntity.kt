package com.potatoes.book5.realestate.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** `scraps` 테이블. 필드 설명은 docs/product/features/F-008-scrap-change-alert.md "저장 데이터 설계". */
@Entity(tableName = "scraps")
data class ScrapEntity(
    @PrimaryKey @ColumnInfo(name = "apartment_id") val apartmentId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "neighborhood") val neighborhood: String,
    @ColumnInfo(name = "area_pyeong") val areaPyeong: Int,
    /** epoch milli */
    @ColumnInfo(name = "scrapped_at") val scrappedAt: Long,
    @ColumnInfo(name = "baseline_price_manwon") val baselinePriceManwon: Long,
    /** epoch day */
    @ColumnInfo(name = "baseline_deal_date") val baselineDealDate: Long,
    @ColumnInfo(name = "last_price_manwon") val lastPriceManwon: Long,
    @ColumnInfo(name = "last_deal_date") val lastDealDate: Long,
    @ColumnInfo(name = "last_checked_at") val lastCheckedAt: Long,
    /** ScrapChangeKind 이름. 변동이 없으면 null */
    @ColumnInfo(name = "change_kind") val changeKind: String?,
    @ColumnInfo(name = "change_delta_manwon") val changeDeltaManwon: Long?,
    @ColumnInfo(name = "change_detected_at") val changeDetectedAt: Long?,
    @ColumnInfo(name = "change_seen") val changeSeen: Boolean,
)
