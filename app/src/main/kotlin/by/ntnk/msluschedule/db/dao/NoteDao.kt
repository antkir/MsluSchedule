package by.ntnk.msluschedule.db.dao

import androidx.room.Dao
import androidx.room.Query
import by.ntnk.msluschedule.db.data.DbNote
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

@Dao
interface NoteDao : BaseDao<DbNote> {
    @Query("SELECT * FROM DbNote WHERE weekdayId=:weekdayId ORDER BY id")
    fun getNotesForWeekdayReactive(weekdayId: Int): Observable<List<DbNote>>

    @Query("DELETE FROM DbNote WHERE id=:id")
    fun delete(id: Int): Completable

    @Query("SELECT EXISTS(SELECT * FROM DbNote WHERE weekdayId=:weekdayId)")
    fun checkWeekdayHasNotes(weekdayId: Int): Single<Boolean>
}
