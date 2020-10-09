package by.ntnk.msluschedule.data

import by.ntnk.msluschedule.utils.EMPTY_STRING

data class TeacherLesson(
        override val subject: String,
        val faculty: String,
        val groups: String,
        override val type: String,
        override val classroom: String,
        override val startTime: String,
        override val endTime: String,
        var id: Int = 0
) : Lesson {
    constructor(startTime: String, endTime: String) : this(
            EMPTY_STRING,
            EMPTY_STRING,
            EMPTY_STRING,
            EMPTY_STRING,
            EMPTY_STRING,
            startTime,
            endTime
    )
}
