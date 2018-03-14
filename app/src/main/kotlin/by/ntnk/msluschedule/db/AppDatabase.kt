package by.ntnk.msluschedule.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import by.ntnk.msluschedule.db.dao.ScheduleContainerDao
import by.ntnk.msluschedule.db.dao.WeekDao
import by.ntnk.msluschedule.db.dao.WeekdayDao
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.db.data.Week
import by.ntnk.msluschedule.db.data.Weekday

@Database(
        entities = [ScheduleContainer::class, Week::class, Weekday::class],
        version = 2,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val scheduleContainerDao: ScheduleContainerDao
    abstract val weekDao: WeekDao
    abstract val weekdayDao: WeekdayDao
}
