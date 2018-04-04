package by.ntnk.msluschedule.data

import by.ntnk.msluschedule.utils.EMPTY_STRING

data class TeacherLesson(
        val subject: String,
        val faculty: String,
        val groups: String,
        val type: String,
        val classroom: String,
        val startTime: String,
        val endTime: String
) {
    constructor(startTime: String, endTime: String): this(
            EMPTY_STRING,
            EMPTY_STRING,
            EMPTY_STRING,
            EMPTY_STRING,
            EMPTY_STRING,
            startTime,
            endTime
    )
}
