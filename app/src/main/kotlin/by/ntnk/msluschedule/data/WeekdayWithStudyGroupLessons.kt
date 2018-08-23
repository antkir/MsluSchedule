package by.ntnk.msluschedule.data

import java.util.Arrays

data class WeekdayWithStudyGroupLessons(override val weekday: String) : WeekdayWithLessons<StudyGroupLesson> {
    override val lessons: MutableList<StudyGroupLesson> = ArrayList()

    constructor(weekday: String, lessons: List<StudyGroupLesson>) : this(weekday) {
        this.lessons.addAll(lessons)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeekdayWithStudyGroupLessons
        if (lessons.size != other.lessons.size) return false
        for (i in lessons.indices) {
            if (lessons[i] != other.lessons[i]) return false
        }

        return true
    }

    override fun hashCode(): Int = Arrays.hashCode(arrayOf(lessons))

    override fun toString(): String {
        var ret = String.format("WeekdayWithStudyGroupLessons(weekday=%s)", weekday)
        for (lesson in lessons) {
            ret += "\n" + lesson.toString()
        }
        return ret
    }
}
