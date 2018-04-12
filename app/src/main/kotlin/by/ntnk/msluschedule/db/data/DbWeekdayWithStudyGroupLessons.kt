package by.ntnk.msluschedule.db.data

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Relation

class DbWeekdayWithStudyGroupLessons {
    @Embedded
    lateinit var weekday: Weekday

    @Relation(parentColumn = "id", entityColumn = "weekdayId")
    lateinit var lessons: List<DbStudyGroupLesson>
}
