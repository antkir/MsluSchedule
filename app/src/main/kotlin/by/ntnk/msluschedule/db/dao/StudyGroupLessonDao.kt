package by.ntnk.msluschedule.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM DbStudyGroupLesson WHERE id=:id")
    fun getLesson(id: Int): Single<DbStudyGroupLesson>

    @Query("SELECT * FROM DbStudyGroupLesson WHERE weekdayId=:weekdayId AND subject=:subject")
    fun getLesson(weekdayId: Int, subject: String): Single<List<DbStudyGroupLesson>>
}
