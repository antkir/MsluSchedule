package by.ntnk.msluschedule.db.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import by.ntnk.msluschedule.data.TeacherLesson

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Weekday::class,
            parentColumns = ["id"],
            childColumns = ["weekdayId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [(Index(value = ["weekdayId"]))]
)
data class DbTeacherLesson constructor(
    val subject: String,
    val faculty: String,
    val groups: String,
    val type: String,
    val classroom: String,
    val startTime: String,
    val endTime: String,
    val weekdayId: Int,
    @PrimaryKey(autoGenerate = true) val id: Int = 0
) {
    @Ignore
    constructor(lesson: TeacherLesson, weekdayId: Int) : this(
        lesson.subject,
        lesson.faculty,
        lesson.groups,
        lesson.type,
        lesson.classroom,
        lesson.startTime,
        lesson.endTime,
        weekdayId
    )
}
