package by.ntnk.msluschedule.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import by.ntnk.msluschedule.db.data.DbStudyGroupLesson
import by.ntnk.msluschedule.db.data.DbWeekdayWithStudyGroupLessons
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface StudyGroupLessonDao : BaseDao<DbStudyGroupLesson> {
    @Query("DELETE FROM DbStudyGroupLesson WHERE weekdayId=:weekdayId")
    fun delete(weekdayId: Int): Completable

    @Transaction
    @Query("SELECT * from Weekday WHERE weekId=:weekId")
    fun getWeekdaysWithLessons(weekId: Int): Single<List<DbWeekdayWithStudyGroupLessons>>

    @Transaction
    @Query("SELECT * from Weekday WHERE id=:id")
    fun getWeekdayWithLessons(id: Int): Single<DbWeekdayWithStudyGroupLessons>

    @Query("SELECT * FROM DbStudyGroupLesson WHERE id=:id")
    fun getLesson(id: Int): Single<DbStudyGroupLesson>

    @Query("SELECT * FROM DbStudyGroupLesson WHERE weekdayId=:weekdayId AND subject=:subject AND type=:type")
    fun getClassesBySubject(weekdayId: Int, subject: String, type: String): Single<List<DbStudyGroupLesson>>
}
