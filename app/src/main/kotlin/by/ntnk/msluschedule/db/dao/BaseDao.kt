package by.ntnk.msluschedule.db.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import io.reactivex.Completable
import io.reactivex.Single

interface BaseDao<in T> {
    @Insert
    fun insert(entity: T): Single<Long>

    @Insert
    fun insert(entity: List<T>): Single<List<Long>>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(entity: T): Completable

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(list: List<T>): Completable

    @Delete
    fun delete(entity: T): Completable

    @Delete
    fun delete(entity: List<T>): Completable
}
