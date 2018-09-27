package by.ntnk.msluschedule.db.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import by.ntnk.msluschedule.db.data.DbNote
import io.reactivex.Single

@Dao
interface NoteDao : BaseDao<DbNote> {
    @Query("SELECT * FROM DbNote WHERE weekdayId=:weekdayId")
    fun getNotesForWeekday(weekdayId: Int): Single<List<DbNote>>

    @Query("DELETE FROM DbNote WHERE id=:id")
    fun delete(id: Int): Int
}
