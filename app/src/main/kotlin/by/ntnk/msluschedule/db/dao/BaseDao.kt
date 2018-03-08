package by.ntnk.msluschedule.db.dao

import android.arch.persistence.room.Delete
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Update

interface BaseDao<in T> {
    @Insert
    fun insert(entity: T): Long

    @Insert
    fun insert(entity: List<T>): List<Long>

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(entity: T)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(list: List<T>)

    @Delete
    fun delete(entity: T)
}
