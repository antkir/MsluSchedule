package by.ntnk.msluschedule.db.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import by.ntnk.msluschedule.db.data.ScheduleContainer
import io.reactivex.Single

@Dao
interface ScheduleContainerDao : BaseDao<ScheduleContainer> {
    @Query("SELECT * FROM ScheduleContainer ORDER BY name")
    fun getScheduleContainers(): Single<List<ScheduleContainer>>

    @Query("DELETE FROM ScheduleContainer WHERE id=:id")
    fun delete(id: Int): Int
}
