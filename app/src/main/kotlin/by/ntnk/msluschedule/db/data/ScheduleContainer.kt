package by.ntnk.msluschedule.db.data

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import by.ntnk.msluschedule.db.ScheduleTypeConverter
import by.ntnk.msluschedule.utils.ScheduleType

@Entity
@TypeConverters(ScheduleTypeConverter::class)
data class ScheduleContainer constructor(
        @PrimaryKey val id: Int,
        val name: String,
        val type: ScheduleType,
        val faculty: Int,
        val course: Int,
        val year: Int
)
