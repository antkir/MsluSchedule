package by.ntnk.msluschedule.db.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.Transaction
import by.ntnk.msluschedule.db.data.DbStudyGroupLesson
import by.ntnk.msluschedule.db.data.DbWeekdayWithStudyGroupLessons
import io.reactivex.Single

@Dao
interface StudyGroupLessonDao : BaseDao<DbStudyGroupLesson> {
    @Query("DELETE FROM DbStudyGroupLesson WHERE weekdayId=:weekdayId")
    fun deleteForWeekday(weekdayId: Int): Int

    @Transaction
    @Query("SELECT * from Weekday WHERE weekId=:weekId")
    fun getWeekdayWithStudyGroupLessonsForWeek(weekId: Int): Single<List<DbWeekdayWithStudyGroupLessons>>
}
