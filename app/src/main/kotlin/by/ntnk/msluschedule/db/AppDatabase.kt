package by.ntnk.msluschedule.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import by.ntnk.msluschedule.db.dao.ScheduleContainerDao
import by.ntnk.msluschedule.db.dao.WeekDao
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.db.data.Week

@Database(
        entities = [ScheduleContainer::class, Week::class],
        version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract val scheduleContainerDao: ScheduleContainerDao
    abstract val weekDao: WeekDao
}
