package by.ntnk.msluschedule.data

data class StudyGroupLesson(
        override val subject: String,
        override val type: String,
        val teacher: String,
        override val classroom: String,
        override val startTime: String,
        override val endTime: String,
        var id: Int = 0
) : Lesson
