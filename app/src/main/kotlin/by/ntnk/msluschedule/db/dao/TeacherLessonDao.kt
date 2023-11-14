package by.ntnk.msluschedule.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import by.ntnk.msluschedule.db.data.DbTeacherLesson
import by.ntnk.msluschedule.db.data.DbWeekdayWithTeacherLessons
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

@Dao
interface TeacherLessonDao : BaseDao<DbTeacherLesson> {
    @Query("DELETE FROM DbTeacherLesson WHERE weekdayId=:weekdayId")
    fun delete(weekdayId: Int): Completable

    @Transaction
    @Query("SELECT * from Weekday WHERE weekId=:weekId")
    fun getWeekdaysWithLessons(weekId: Int): Single<List<DbWeekdayWithTeacherLessons>>

    @Transaction
    @Query("SELECT * from Weekday WHERE id=:id")
    fun getWeekdayWithLessons(id: Int): Single<DbWeekdayWithTeacherLessons>

    @Query("SELECT * FROM DbTeacherLesson WHERE id=:id")
    fun getLesson(id: Int): Single<DbTeacherLesson>

    @Query("SELECT * FROM DbTeacherLesson WHERE weekdayId=:weekdayId AND subject=:subject AND groups=:groups")
    fun getClassesBySubject(weekdayId: Int, subject: String, groups: String): Single<List<DbTeacherLesson>>
}
