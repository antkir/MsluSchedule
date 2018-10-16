package by.ntnk.msluschedule.db.dao

import androidx.room.Dao
import androidx.room.Query
import by.ntnk.msluschedule.db.data.Weekday
import io.reactivex.Single

@Dao
interface WeekdayDao : BaseDao<Weekday> {
    @Query("SELECT * FROM Weekday WHERE weekId=:weekId")
    fun getWeekdaysForWeek(weekId: Int): Single<List<Weekday>>

    @Query("SELECT * FROM Weekday WHERE id=:id")
    fun getWeekday(id: Int): Single<Weekday>
}
