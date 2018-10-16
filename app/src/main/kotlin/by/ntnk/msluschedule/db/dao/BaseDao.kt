package by.ntnk.msluschedule.db.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

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

    @Delete
    fun delete(entity: List<T>)
}
