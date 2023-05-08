package by.ntnk.msluschedule.data

import by.ntnk.msluschedule.utils.DEFAULT

data class WeekdayWithStudyGroupLessons(
    override val weekdayId: Int = Int.DEFAULT,
    override val weekday: String,
    override val classes: MutableList<StudyGroupLesson> = mutableListOf()
) : WeekdayWithLessons<StudyGroupLesson>
