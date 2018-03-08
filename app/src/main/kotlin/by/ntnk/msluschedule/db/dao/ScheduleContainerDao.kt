package by.ntnk.msluschedule.db.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import by.ntnk.msluschedule.db.data.ScheduleContainer
import io.reactivex.Single

@Dao
interface ScheduleContainerDao : BaseDao<ScheduleContainer> {
    @Query("SELECT * FROM ScheduleContainer")
    fun getAllScheduleContainers(): Single<List<ScheduleContainer>>
}
