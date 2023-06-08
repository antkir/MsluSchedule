package by.ntnk.msluschedule.data

data class TeacherLesson(
    override val id: Int,
    override val subject: String,
    override val type: String,
    override val classroom: String,
    override val startTime: String,
    override val endTime: String,
    val faculty: String,
    val groups: String
) : Lesson
