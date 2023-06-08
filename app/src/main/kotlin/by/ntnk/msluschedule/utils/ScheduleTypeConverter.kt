package by.ntnk.msluschedule.utils

import androidx.room.TypeConverter

class ScheduleTypeConverter {
    companion object {
        @TypeConverter
        @JvmStatic
        fun fromString(value: String): ScheduleType {
            return when (value) {
                ScheduleType.STUDYGROUP.name -> ScheduleType.STUDYGROUP
                ScheduleType.TEACHER.name -> ScheduleType.TEACHER
                else -> ScheduleType.DEFAULT
            }
        }

        @TypeConverter
        @JvmStatic
        fun toString(type: ScheduleType): String {
            return type.name
        }
    }
}
