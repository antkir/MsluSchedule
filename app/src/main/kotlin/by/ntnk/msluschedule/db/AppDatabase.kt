package by.ntnk.msluschedule.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import by.ntnk.msluschedule.db.dao.ScheduleContainerDao
import by.ntnk.msluschedule.db.data.ScheduleContainer

@Database(
        entities = [ScheduleContainer::class],
        version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract val scheduleContainerDao: ScheduleContainerDao
}
