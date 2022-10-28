package by.ntnk.msluschedule.db

import androidx.room.Database
import androidx.room.RoomDatabase
import by.ntnk.msluschedule.db.dao.NoteDao
import by.ntnk.msluschedule.db.dao.ScheduleContainerDao
import by.ntnk.msluschedule.db.dao.StudyGroupLessonDao
import by.ntnk.msluschedule.db.dao.TeacherLessonDao
import by.ntnk.msluschedule.db.dao.WeekDao
import by.ntnk.msluschedule.db.dao.WeekdayDao
import by.ntnk.msluschedule.db.data.DbNote
import by.ntnk.msluschedule.db.data.DbStudyGroupLesson
import by.ntnk.msluschedule.db.data.DbTeacherLesson
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.db.data.Week
import by.ntnk.msluschedule.db.data.Weekday

@Database(
    entities = [
        ScheduleContainer::class,
        Week::class,
        Weekday::class,
        DbStudyGroupLesson::class,
        DbTeacherLesson::class,
        DbNote::class
    ],
    version = 10
)
abstract class AppDatabase : RoomDatabase() {
    abstract val scheduleContainerDao: ScheduleContainerDao
    abstract val weekDao: WeekDao
    abstract val weekdayDao: WeekdayDao
    abstract val studyGroupLessonDao: StudyGroupLessonDao
    abstract val teacherLessonDao: TeacherLessonDao
    abstract val noteDao: NoteDao
}
