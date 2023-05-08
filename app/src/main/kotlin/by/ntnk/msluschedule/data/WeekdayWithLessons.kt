package by.ntnk.msluschedule.data

interface WeekdayWithLessons<out T : Lesson> {
    val weekdayId: Int
    val weekday: String
    val classes: List<T>
}
