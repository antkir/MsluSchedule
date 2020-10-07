package by.ntnk.msluschedule.db.data

import androidx.room.*
import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.utils.EMPTY_STRING

@Entity(
        foreignKeys = [
            (ForeignKey(
                    entity = Weekday::class,
                    parentColumns = ["id"],
                    childColumns = ["weekdayId"],
                    onDelete = ForeignKey.CASCADE
            ))
        ],
        indices = [(Index(value = ["weekdayId"]))]
)
data class DbStudyGroupLesson constructor(
        val subject: String,
        val type: String,
        val teacher: String,
        val classroom: String,
        val startTime: String,
        val endTime: String,
        val weekdayId: Int,
        @PrimaryKey(autoGenerate = true) val id: Int = 0
) {
    @Ignore
    constructor(lesson: StudyGroupLesson, weekdayId: Int) : this(
            lesson.subject,
            EMPTY_STRING,
            lesson.teacher,
            lesson.classroom,
            lesson.startTime,
            lesson.endTime,
            weekdayId
    )
}
