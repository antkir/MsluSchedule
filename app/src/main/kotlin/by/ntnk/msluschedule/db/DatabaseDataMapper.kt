package by.ntnk.msluschedule.db

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.db.data.Week
import by.ntnk.msluschedule.db.data.Weekday
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.*
import java.util.Arrays
import javax.inject.Inject

@PerApp
class DatabaseDataMapper @Inject constructor() {
    fun map(data: StudyGroup): ScheduleContainer =
            ScheduleContainer(data.id, data.name, ScheduleType.STUDYGROUP, data.faculty, data.course, data.year)

    fun map(data: Teacher): ScheduleContainer =
            ScheduleContainer(data.id, data.name, ScheduleType.TEACHER, 0, 0, data.year)

    fun map(data: ScheduleFilter, containerId: Int): List<Week> {
        val weeks = ArrayList<Week>()
        for (i in 0 until data.size) {
            val key = data.keyAt(i)
            val value = data.valueAt(i)
            val week = Week(containerId, key, value)
            weeks.add(week)
        }
        return weeks
    }

    fun createWeekDaysList(weekId: Int): List<Weekday> =
            Arrays.asList(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
                    .map { weekdayValue -> Weekday(weekId, weekdayValue) }
                    .toList()
}
