package by.ntnk.msluschedule.data

import by.ntnk.msluschedule.utils.DEFAULT

data class WeekdayWithTeacherLessons(
    override val weekdayId: Int = Int.DEFAULT,
    override val weekday: String,
    override val lessons : MutableList<TeacherLesson> = mutableListOf()
) : WeekdayWithLessons<TeacherLesson>
