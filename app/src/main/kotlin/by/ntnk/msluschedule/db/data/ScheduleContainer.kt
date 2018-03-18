package by.ntnk.msluschedule.db.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import by.ntnk.msluschedule.utils.ScheduleTypeConverter
import by.ntnk.msluschedule.utils.ScheduleType

@Entity
@TypeConverters(ScheduleTypeConverter::class)
data class ScheduleContainer constructor(
        val key: Int,
        val name: String,
        val type: ScheduleType,
        val year: Int,
        val faculty: Int = 0,
        val course: Int = 0,
        @PrimaryKey(autoGenerate = true) val id: Int = 0
)
