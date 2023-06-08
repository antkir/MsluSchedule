package by.ntnk.msluschedule.data

interface WeekdayWithLessons<out T : Lesson> {
    val id: Int
    val value: String
    val classes: List<T>
}
