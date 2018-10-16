package by.ntnk.msluschedule.db.data

import androidx.room.Embedded
import androidx.room.Relation

class DbWeekdayWithStudyGroupLessons {
    @Embedded
    lateinit var weekday: Weekday

    @Relation(parentColumn = "id", entityColumn = "weekdayId")
    lateinit var lessons: List<DbStudyGroupLesson>
}
