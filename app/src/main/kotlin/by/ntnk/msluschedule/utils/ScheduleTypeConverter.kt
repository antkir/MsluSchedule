package by.ntnk.msluschedule.utils

import androidx.room.TypeConverter

class ScheduleTypeConverter {
    companion object {
        @TypeConverter
        @JvmStatic
        fun stringToScheduleType(value: String): ScheduleType {
            return when (value) {
                ScheduleType.STUDYGROUP.name -> ScheduleType.STUDYGROUP
                ScheduleType.TEACHER.name -> ScheduleType.TEACHER
                else -> ScheduleType.DEFAULT
            }
        }

        @TypeConverter
        @JvmStatic
        fun scheduleTypeToString(type: ScheduleType): String {
            return type.name
        }
    }
}
