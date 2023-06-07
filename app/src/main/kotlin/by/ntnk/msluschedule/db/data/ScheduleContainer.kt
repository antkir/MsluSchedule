package by.ntnk.msluschedule.db.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import by.ntnk.msluschedule.utils.AUTO_ID
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.ScheduleTypeConverter

@Entity
@TypeConverters(ScheduleTypeConverter::class)
data class ScheduleContainer constructor(
    val key: Int,
    val name: String,
    val type: ScheduleType,
    val year: Int,
    val faculty: Int,
    val course: Int,
    @PrimaryKey(autoGenerate = true) val id: Int = AUTO_ID
)
