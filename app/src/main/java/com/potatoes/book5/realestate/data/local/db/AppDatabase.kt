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
 */
@Database(
    entities = [ScrapEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scrapDao(): ScrapDao

    companion object {
        const val NAME = "im_with_you.db"

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME).build()
    }
}
