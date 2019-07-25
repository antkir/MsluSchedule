package by.ntnk.msluschedule.db.dao

import androidx.room.Dao
import androidx.room.Query
import by.ntnk.msluschedule.db.data.DbNote
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface NoteDao : BaseDao<DbNote> {
    @Query("SELECT * FROM DbNote WHERE weekdayId=:weekdayId ORDER BY subject DESC")
    fun getNotesForWeekday(weekdayId: Int): Single<List<DbNote>>

    @Query("DELETE FROM DbNote WHERE id=:id")
    fun delete(id: Int): Completable
}
