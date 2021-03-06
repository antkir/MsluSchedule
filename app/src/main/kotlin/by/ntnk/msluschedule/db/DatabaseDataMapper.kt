package by.ntnk.msluschedule.db

import by.ntnk.msluschedule.data.*
import by.ntnk.msluschedule.db.data.*
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.*
import javax.inject.Inject

@PerApp
class DatabaseDataMapper @Inject constructor() {
    fun mapToStudyGroup(data: ScheduleContainer): StudyGroup =
            StudyGroup(data.key, data.name, data.faculty, data.course, data.year)

    fun mapToTeacher(data: ScheduleContainer): Teacher =
            Teacher(data.key, data.name, data.year)

    fun map(data: ScheduleFilter, containerId: Int): List<Week> {
        val weeks = ArrayList<Week>(data.size)
        for (i in 0 until data.size) {
            val key = data.keyAt(i)
            val value = data.valueAt(i)
            val week = Week(key, value, containerId)
            weeks.add(week)
        }
        return weeks
    }

    fun createWeekdayList(weekId: Int): List<Weekday> =
            listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
                    .map { weekdayValue -> Weekday(weekdayValue, weekId) }
                    .toList()

    fun map(weekdayWithLessons: DbWeekdayWithTeacherLessons): WeekdayWithTeacherLessons {
        val lessons = ArrayList<TeacherLesson>(weekdayWithLessons.lessons.size)
        for (lesson in weekdayWithLessons.lessons) {
            lessons.add(
                    TeacherLesson(
                            lesson.subject,
                            lesson.faculty,
                            lesson.groups,
                            lesson.type,
                            lesson.classroom,
                            lesson.startTime,
                            lesson.endTime,
                            lesson.id
                    )
            )
        }
        return WeekdayWithTeacherLessons(weekdayWithLessons.weekday.id, weekdayWithLessons.weekday.value, lessons)
    }

    fun map(weekdayWithLessons: DbWeekdayWithStudyGroupLessons): WeekdayWithStudyGroupLessons {
        val lessons = ArrayList<StudyGroupLesson>(weekdayWithLessons.lessons.size)
        for (lesson in weekdayWithLessons.lessons) {
            lessons.add(
                    StudyGroupLesson(
                            lesson.subject,
                            lesson.type,
                            lesson.teacher,
                            lesson.classroom,
                            lesson.startTime,
                            lesson.endTime,
                            lesson.id
                    )
            )
        }
        return WeekdayWithStudyGroupLessons(weekdayWithLessons.weekday.id, weekdayWithLessons.weekday.value, lessons)
    }
}
