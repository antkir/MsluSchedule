package by.ntnk.msluschedule.data

data class WeekdayWithTeacherLessons(
    override val id: Int,
    override val value: String,
    override val classes : MutableList<TeacherLesson> = mutableListOf()
) : WeekdayWithLessons<TeacherLesson>
