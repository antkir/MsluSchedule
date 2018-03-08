package by.ntnk.msluschedule.di.modules

import android.arch.persistence.room.Room
import by.ntnk.msluschedule.MsluScheduleApp
import by.ntnk.msluschedule.db.AppDatabase
import by.ntnk.msluschedule.di.PerApp

import dagger.Module
import dagger.Provides

const val DATABASE_NAME = "application_db"

@Module
class DatabaseModule {
    @PerApp
    @Provides
    fun provideAppDatabase(app: MsluScheduleApp): AppDatabase {
        return Room.databaseBuilder(app, AppDatabase::class.java, DATABASE_NAME)
                .build()
    }
}
