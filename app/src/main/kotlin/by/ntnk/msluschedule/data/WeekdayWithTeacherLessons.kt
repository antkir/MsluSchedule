package by.ntnk.msluschedule.data

import by.ntnk.msluschedule.utils.INVALID_VALUE

data class WeekdayWithTeacherLessons(override val weekday: String) : WeekdayWithLessons<TeacherLesson> {
    override var weekdayId: Int = INVALID_VALUE
        private set

    override val lessons = mutableListOf<TeacherLesson>()

    constructor(weekdayId: Int, weekday: String, lessons: List<TeacherLesson>) : this(weekday) {
        this.weekdayId = weekdayId
        this.lessons.addAll(lessons)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeekdayWithTeacherLessons
        if (lessons.size != other.lessons.size) return false
        for (i in lessons.indices) {
            if (lessons[i] != other.lessons[i]) return false
        }

        return true
    }

    override fun hashCode(): Int = arrayOf(lessons).contentHashCode()

    override fun toString(): String {
        var ret = "WeekdayWithTeacherLessons(weekday=$weekday)"
        for (lesson in lessons) {
            ret += "\n$lesson"
        }
        return ret
    }
}
