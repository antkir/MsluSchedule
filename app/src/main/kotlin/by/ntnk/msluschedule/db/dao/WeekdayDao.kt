package by.ntnk.msluschedule.db.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import by.ntnk.msluschedule.db.data.Weekday
import io.reactivex.Single

@Dao
interface WeekdayDao : BaseDao<Weekday> {
    @Query("SELECT * FROM Weekday WHERE weekId=:weekId")
    fun getWeekdaysForWeek(weekId: Int): Single<List<Weekday>>
}
