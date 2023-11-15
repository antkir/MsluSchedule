package by.ntnk.msluschedule.di.modules

import android.content.Context
import androidx.room.Room
import by.ntnk.msluschedule.db.AppDatabase
import by.ntnk.msluschedule.db.Migrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext applicationContext: Context): AppDatabase {
        return Room.databaseBuilder(applicationContext, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(
                Migrations.MIGRATION_8_9,
                Migrations.MIGRATION_9_10,
                Migrations.MIGRATION_10_11
            )
            .build()
    }

    private companion object {
        private const val DATABASE_NAME = "application_db"
    }
}
