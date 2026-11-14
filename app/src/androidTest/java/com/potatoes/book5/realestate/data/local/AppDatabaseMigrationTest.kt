package com.potatoes.book5.realestate.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.potatoes.book5.realestate.data.local.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** L2: v1에서 저장한 스크랩이 v2(T-017)로 옮겨져도 유지되고 새 열은 비어 있다. */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun migrate1To2KeepsScrapsAndAddsEmptyComplexColumns() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO scraps (apartment_id, name, neighborhood, area_pyeong, scrapped_at,
                    baseline_price_manwon, baseline_deal_date, last_price_manwon, last_deal_date, last_checked_at,
                    change_kind, change_delta_manwon, change_detected_at, change_seen)
                VALUES ('a', '단지-a', '정자동', 34, 1000, 150000, 20000, 153000, 20007, 2000, 'NEW_DEAL', 3000, 2000, 0)
                """.trimIndent()
            )
        }
        val db = helper.runMigrationsAndValidate(DB, 2, true)
        db.query("SELECT name, last_price_manwon, change_kind, households, built_year FROM scraps").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("단지-a", c.getString(0))
            assertEquals(153000L, c.getLong(1))
            assertEquals("NEW_DEAL", c.getString(2))
            assertTrue(c.isNull(3))
            assertTrue(c.isNull(4))
        }
        db.close()
    }

    @Test
    fun migrate2To3AddsEmptyDriveTables() {
        helper.createDatabase(DB, 2).close()
        val db = helper.runMigrationsAndValidate(DB, 3, true)
        listOf("drive_sessions", "drive_ranks", "drive_leader_changes", "drive_passed_interests").forEach { table ->
            db.query("SELECT COUNT(*) FROM $table").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(0, c.getInt(0))
            }
        }
        db.close()
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
