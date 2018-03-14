package by.ntnk.msluschedule.db

import android.arch.persistence.room.TypeConverter
import by.ntnk.msluschedule.utils.ScheduleType

class ScheduleTypeConverter {
    @TypeConverter
    fun stringToScheduleType(value: String?): ScheduleType? {
        return when(value) {
            ScheduleType.STUDYGROUP.name -> ScheduleType.STUDYGROUP
            ScheduleType.TEACHER.name -> ScheduleType.TEACHER
            else -> null
        }
    }

    @TypeConverter
    fun scheduleTypeToString(type: ScheduleType?): String? {
        return when(type) {
            ScheduleType.STUDYGROUP, ScheduleType.TEACHER -> type.name
            else -> null
        }
    }
}
