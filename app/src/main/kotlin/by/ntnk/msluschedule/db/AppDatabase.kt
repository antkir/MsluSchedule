package by.ntnk.msluschedule.db

import androidx.room.Database
import androidx.room.RoomDatabase
import by.ntnk.msluschedule.db.dao.*
import by.ntnk.msluschedule.db.data.*

@Database(
        entities = [
            ScheduleContainer::class,
            Week::class,
            Weekday::class,
            DbStudyGroupLesson::class,
            DbTeacherLesson::class,
            DbNote::class
        ],
        version = 9
)
abstract class AppDatabase : RoomDatabase() {
    abstract val scheduleContainerDao: ScheduleContainerDao
    abstract val weekDao: WeekDao
    abstract val weekdayDao: WeekdayDao
    abstract val studyGroupLessonDao: StudyGroupLessonDao
    abstract val teacherLessonDao: TeacherLessonDao
    abstract val noteDao: NoteDao
}
