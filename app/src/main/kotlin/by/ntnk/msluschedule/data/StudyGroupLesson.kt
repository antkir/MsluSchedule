package by.ntnk.msluschedule.data

data class StudyGroupLesson(
    override val id: Int,
    override val subject: String,
    override val type: String,
    override val classroom: String,
    override val startTime: String,
    override val endTime: String,
    val teacher: String
) : Lesson
