package com.potatoes.book5.realestate.data.local

import com.potatoes.book5.realestate.data.local.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Remove only records explicitly owned by the old sample data set. Real user records remain. */
class LegacySampleCleanup(private val database: AppDatabase) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        database.runInTransaction {
            val db = database.openHelper.writableDatabase
            db.execSQL("DELETE FROM scraps WHERE apartment_id LIKE 'sample-%'")
            db.execSQL("""DELETE FROM drive_sessions WHERE id IN (
                SELECT session_id FROM drive_ranks WHERE apartment_id LIKE 'sample-%'
                UNION SELECT session_id FROM drive_passed_interests WHERE apartment_id LIKE 'sample-%'
                UNION SELECT session_id FROM drive_leader_changes WHERE apartment_id LIKE 'sample-%'
            )""".trimIndent())
        }
    }
}
