package by.ntnk.msluschedule.data

data class WeekdayWithStudyGroupLessons(
    override val id: Int,
    override val value: String,
    override val classes: MutableList<StudyGroupLesson> = mutableListOf()
) : WeekdayWithLessons<StudyGroupLesson>
