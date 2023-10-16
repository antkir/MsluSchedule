package by.ntnk.msluschedule.di.modules

import androidx.room.Room
import by.ntnk.msluschedule.MsluScheduleApp
import by.ntnk.msluschedule.db.AppDatabase
import by.ntnk.msluschedule.db.Migrations
import by.ntnk.msluschedule.di.PerApp
import dagger.Module
import dagger.Provides

@Module
class DatabaseModule {
    @PerApp
    @Provides
    fun provideAppDatabase(app: MsluScheduleApp): AppDatabase {
        return Room.databaseBuilder(app, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(
                Migrations.MIGRATION_8_9,
                Migrations.MIGRATION_9_10
            )
            .build()
    }

    private companion object {
        private const val DATABASE_NAME = "application_db"
    }
}
