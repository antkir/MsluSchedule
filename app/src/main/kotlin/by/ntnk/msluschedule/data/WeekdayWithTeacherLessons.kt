package by.ntnk.msluschedule.data

import java.util.Arrays

data class WeekdayWithTeacherLessons(override val weekday: String) : WeekdayWithLessons<TeacherLesson> {
    override var weekdayId: Int = -1
        private set

    override val lessons: MutableList<TeacherLesson> = ArrayList()

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

    override fun hashCode(): Int = Arrays.hashCode(arrayOf(lessons))

    override fun toString(): String {
        var ret = String.format("WeekdayWithTeacherLessons(weekday=%s)", weekday)
        for (lesson in lessons) {
            ret += "\n" + lesson.toString()
        }
        return ret
    }
}
