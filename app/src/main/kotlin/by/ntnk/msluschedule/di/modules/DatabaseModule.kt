package by.ntnk.msluschedule.di.modules

import androidx.room.Room
import by.ntnk.msluschedule.MsluScheduleApp
import by.ntnk.msluschedule.db.AppDatabase
import by.ntnk.msluschedule.di.PerApp

import dagger.Module
import dagger.Provides
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration

const val DATABASE_NAME = "application_db"

@Module
class DatabaseModule {
    private val migration8to9: Migration = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE DbNote ADD COLUMN subject VARCHAR DEFAULT '' NOT NULL")
        }
    }

    private val migration9to10: Migration = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE DbStudyGroupLesson ADD COLUMN type VARCHAR DEFAULT '' NOT NULL")
        }
    }

    @PerApp
    @Provides
    fun provideAppDatabase(app: MsluScheduleApp): AppDatabase {
        return Room.databaseBuilder(app, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(migration8to9)
                .addMigrations(migration9to10)
                .build()
    }
}
