package by.ntnk.msluschedule.data

data class StudyGroupLesson(
        val subject: String,
        val teacher: String,
        val classroom: String,
        val startTime: String,
        val endTime: String,
        var id: Int = 0
) : Lesson
