package by.ntnk.msluschedule.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE DbNote ADD COLUMN subject VARCHAR DEFAULT '' NOT NULL")
        }
    }

    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE DbStudyGroupLesson ADD COLUMN type VARCHAR DEFAULT '' NOT NULL")
        }
    }
}