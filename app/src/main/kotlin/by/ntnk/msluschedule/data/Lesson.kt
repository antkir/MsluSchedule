package by.ntnk.msluschedule.data

interface Lesson {
    val id: Int
    val subject: String
    val type: String
    val classroom: String
    val startTime: String
    val endTime: String
}
