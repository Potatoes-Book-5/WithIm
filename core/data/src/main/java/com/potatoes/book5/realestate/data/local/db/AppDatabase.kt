package com.potatoes.book5.realestate.data.local.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * 앱 로컬 DB(ADR-005). 스키마는 app/schemas에 내보낸다.
 * 버전을 올릴 때는 마이그레이션을 함께 추가한다.
 * - v2(T-017): scraps에 households, built_year 추가(값 없음으로 시작)
 * - v3(T-018): 주행 요약 테이블 4개 추가
 * - v5(T-037): 세션 추천 후보와 main_reco_key 추가
 * - v6(T-037): 추천 키워드 문맥(동네·세대수·준공연도) 추가
 * - v7(T-040): 추천 상세 열기 반응 집계 추가
 */
@Database(
    entities = [
        ScrapEntity::class,
        DriveSessionEntity::class,
        DriveRankEntity::class,
        DriveLeaderChangeEntity::class,
        DrivePassedInterestEntity::class,
        DriveRecommendationEntity::class,
        RecommendationFeedbackEntity::class,
        DriveRegionRoundEntity::class,
        DriveRegionRankEntity::class,
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5), AutoMigration(from = 5, to = 6), AutoMigration(from = 6, to = 7)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scrapDao(): ScrapDao
    abstract fun driveDao(): DriveDao
    abstract fun recommendationFeedbackDao(): RecommendationFeedbackDao

    companion object {
        const val NAME = "im_with_you.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME).build()
    }
}
