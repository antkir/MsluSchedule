package by.ntnk.msluschedule.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import by.ntnk.msluschedule.db.data.DbTeacherLesson
import by.ntnk.msluschedule.db.data.DbWeekdayWithTeacherLessons
import io.reactivex.Single

@Dao
interface TeacherLessonDao : BaseDao<DbTeacherLesson> {
    @Query("DELETE FROM DbTeacherLesson WHERE weekdayId=:weekdayId")
    fun deleteForWeekday(weekdayId: Int): Int

    @Transaction
    @Query("SELECT * from Weekday WHERE weekId=:weekId")
    fun getWeekdayWithTeacherLessonsForWeek(weekId: Int): Single<List<DbWeekdayWithTeacherLessons>>

    @Query("SELECT * FROM DbTeacherLesson WHERE id=:id")
    fun getLesson(id: Int): Single<DbTeacherLesson>

    @Query("SELECT * FROM DbTeacherLesson WHERE weekdayId=:weekdayId AND groups=:groups")
    fun getLesson(weekdayId: Int, groups: String): Single<List<DbTeacherLesson>>
}
