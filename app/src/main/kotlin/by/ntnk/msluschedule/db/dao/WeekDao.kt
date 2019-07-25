package by.ntnk.msluschedule.db.dao

import androidx.room.Dao
import androidx.room.Query
import by.ntnk.msluschedule.db.data.Week
import io.reactivex.Single

@Dao
interface WeekDao : BaseDao<Week> {
    @Query("SELECT * FROM Week WHERE containerId=:containerId ORDER BY `key`")
    fun getWeeks(containerId: Int): Single<List<Week>>

    @Query("SELECT * FROM Week WHERE id=:id")
    fun getWeek(id: Int): Single<Week>
}
