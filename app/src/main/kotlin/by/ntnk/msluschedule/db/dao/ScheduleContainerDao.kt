package by.ntnk.msluschedule.db.dao

import androidx.room.Dao
import androidx.room.Query
import by.ntnk.msluschedule.db.data.ScheduleContainer
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface ScheduleContainerDao : BaseDao<ScheduleContainer> {
    @Query("SELECT * FROM ScheduleContainer ORDER BY name")
    fun getScheduleContainers(): Single<List<ScheduleContainer>>

    @Query("SELECT * FROM ScheduleContainer WHERE id=:id")
    fun getScheduleContainer(id: Int): Single<ScheduleContainer>

    @Query("DELETE FROM ScheduleContainer WHERE id=:id")
    fun delete(id: Int): Completable
}
