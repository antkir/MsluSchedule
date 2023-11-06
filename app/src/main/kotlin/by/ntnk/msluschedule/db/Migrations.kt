package by.ntnk.msluschedule.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE DbNote ADD COLUMN subject VARCHAR DEFAULT '' NOT NULL")
        }
    }

    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE DbStudyGroupLesson ADD COLUMN type VARCHAR DEFAULT '' NOT NULL")
        }
    }

    // Myuniversity API serves group codes not only as integers, but also as strings (e.g. "1000-7").
    // We tried to transform and store such keys as integer numbers,
    // but it turns out they also use non-digit characters there (e.g. "1000-7s"),
    // so just store schedule container keys as TEXT in the app's database.
    val MIGRATION_10_11: Migration = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS new_ScheduleContainer")
            db.execSQL(
                """
                CREATE TABLE new_ScheduleContainer (
                    key TEXT NOT NULL,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    faculty INTEGER NOT NULL,
                    course INTEGER NOT NULL,
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
                )
                """.trimIndent()
            )

            val cursor = db.query("SELECT * FROM ScheduleContainer ORDER BY id")
            for (i in 0..<cursor.count) {
                cursor.moveToPosition(i)

                val keyIndex = cursor.getColumnIndex("key")
                val nameIndex = cursor.getColumnIndex("name")
                val typeIndex = cursor.getColumnIndex("type")
                val yearIndex = cursor.getColumnIndex("year")
                val facultyIndex = cursor.getColumnIndex("faculty")
                val courseIndex = cursor.getColumnIndex("course")
                val idIndex = cursor.getColumnIndex("id")

                val values = ContentValues()
                    .apply {
                        put("key", scheduleContainerKeyToString(cursor.getInt(keyIndex)))
                        put("name", cursor.getString(nameIndex))
                        put("type", cursor.getString(typeIndex))
                        put("year", cursor.getInt(yearIndex))
                        put("faculty", cursor.getInt(facultyIndex))
                        put("course", cursor.getInt(courseIndex))
                        put("id", cursor.getInt(idIndex))
                    }

                db.insert("new_ScheduleContainer", SQLiteDatabase.CONFLICT_REPLACE, values)
            }
            cursor.close()

            db.execSQL("DROP TABLE ScheduleContainer")
            db.execSQL("ALTER TABLE new_ScheduleContainer RENAME TO ScheduleContainer")
        }

        private val GROUP_CODE_OFFSET = 100
        private fun scheduleContainerKeyToString(groupKey: Int): String {
            return if (groupKey >= 0) {
                groupKey.toString(10)
            } else {
                val groupCode = -groupKey
                val code = groupCode / GROUP_CODE_OFFSET
                val subcode = groupCode % GROUP_CODE_OFFSET
                "$code-$subcode"
            }
        }
    }
}
