package by.ntnk.msluschedule.data

interface WeekdayWithLessons<out T : Lesson> {
    val weekday: String
    val lessons: List<T>
}
