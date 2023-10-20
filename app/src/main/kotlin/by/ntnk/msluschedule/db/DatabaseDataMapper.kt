package by.ntnk.msluschedule.db

import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.db.data.DbWeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.db.data.DbWeekdayWithTeacherLessons
import by.ntnk.msluschedule.db.data.Week
import by.ntnk.msluschedule.db.data.Weekday
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.Days
import javax.inject.Inject

@PerApp
class DatabaseDataMapper @Inject constructor() {
    fun map(data: ScheduleFilter, containerId: Int): List<Week> {
        val weeks = ArrayList<Week>(data.size)
        for (i in 0 until data.size) {
            val key = data.keyAt(i).toInt()
            val value = data.valueAt(i)
            val week = Week(key, value, containerId)
            weeks.add(week)
        }
        return weeks
    }

    fun createWeekdayList(weekId: Int): List<Weekday> =
        Days.list()
            .map { weekdayValue -> Weekday(weekdayValue, weekId) }
            .toList()

    fun map(weekdayWithClasses: DbWeekdayWithTeacherLessons): WeekdayWithTeacherLessons {
        val classes = ArrayList<TeacherLesson>(weekdayWithClasses.lessons.size)
        for (classDescriptor in weekdayWithClasses.lessons) {
            classes.add(
                TeacherLesson(
                    classDescriptor.id,
                    classDescriptor.subject,
                    classDescriptor.type,
                    classDescriptor.classroom,
                    classDescriptor.startTime,
                    classDescriptor.endTime,
                    classDescriptor.faculty,
                    classDescriptor.groups
                )
            )
        }
        return WeekdayWithTeacherLessons(weekdayWithClasses.weekday.id, weekdayWithClasses.weekday.value, classes)
    }

    fun map(weekdayWithClasses: DbWeekdayWithStudyGroupLessons): WeekdayWithStudyGroupLessons {
        val classes = ArrayList<StudyGroupLesson>(weekdayWithClasses.lessons.size)
        for (classDescriptor in weekdayWithClasses.lessons) {
            classes.add(
                StudyGroupLesson(
                    classDescriptor.id,
                    classDescriptor.subject,
                    classDescriptor.type,
                    classDescriptor.classroom,
                    classDescriptor.startTime,
                    classDescriptor.endTime,
                    classDescriptor.teacher
                )
            )
        }
        return WeekdayWithStudyGroupLessons(weekdayWithClasses.weekday.id, weekdayWithClasses.weekday.value, classes)
    }
}
