package by.ntnk.msluschedule.db.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import by.ntnk.msluschedule.db.data.Week
import io.reactivex.Single

@Dao
interface WeekDao : BaseDao<Week> {
    @Query("SELECT * FROM Week WHERE containerId=:containerId ORDER BY `key`")
    fun getWeeksForContainer(containerId: Int): Single<List<Week>>

    @Query("SELECT * FROM Week WHERE id=:id")
    fun getWeek(id: Int): Single<Week>
}
