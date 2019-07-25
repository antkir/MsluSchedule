package by.ntnk.msluschedule.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import by.ntnk.msluschedule.db.data.DbTeacherLesson
import by.ntnk.msluschedule.db.data.DbWeekdayWithTeacherLessons
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single

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
    fun getLesson(id: Int): Maybe<DbTeacherLesson>

    @Query("SELECT * FROM DbTeacherLesson WHERE weekdayId=:weekdayId AND groups=:groups")
    fun getLessons(weekdayId: Int, groups: String): Single<List<DbTeacherLesson>>
}
