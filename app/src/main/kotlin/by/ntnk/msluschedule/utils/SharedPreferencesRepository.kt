package by.ntnk.msluschedule.utils

import android.content.SharedPreferences
import android.support.v7.app.AppCompatDelegate
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.di.PerApp

import javax.inject.Inject

const val SCHEDULECONTAINER_ID = "scheduleContainerId"
const val SCHEDULECONTAINER_VALUE = "scheduleContainerValue"
const val SCHEDULECONTAINER_TYPE = "scheduleContainerType"

@PerApp
class SharedPreferencesRepository @Inject constructor(private val sharedPreferences: SharedPreferences) {
    fun getSelectedScheduleContainerInfo(): ScheduleContainerInfo {
        val id = sharedPreferences.getInt(SCHEDULECONTAINER_ID, 0)
        val value = sharedPreferences.getString(SCHEDULECONTAINER_VALUE, EMPTY_STRING)
        val typeString = sharedPreferences.getString(SCHEDULECONTAINER_TYPE, null)
        val type = ScheduleTypeConverter.stringToScheduleType(typeString)
        return ScheduleContainerInfo(id, value, type)
    }

    fun putSelectedScheduleContainer(id: Int, value: String, type: ScheduleType) {
        val typeString = ScheduleTypeConverter.scheduleTypeToString(type)
        sharedPreferences.edit()
                .putInt(SCHEDULECONTAINER_ID, id)
                .putString(SCHEDULECONTAINER_VALUE, value)
                .putString(SCHEDULECONTAINER_TYPE, typeString)
                .apply()
    }

    fun putSelectedScheduleContainer(scheduleContainerInfo: ScheduleContainerInfo) {
        val typeString = ScheduleTypeConverter.scheduleTypeToString(scheduleContainerInfo.type)
        sharedPreferences.edit()
                .putInt(SCHEDULECONTAINER_ID, scheduleContainerInfo.id)
                .putString(SCHEDULECONTAINER_VALUE, scheduleContainerInfo.value)
                .putString(SCHEDULECONTAINER_TYPE, typeString)
                .apply()
    }

    fun selectEmptyScheduleContainer() {
        sharedPreferences.edit()
                .putInt(SCHEDULECONTAINER_ID, 0)
                .putString(SCHEDULECONTAINER_VALUE, EMPTY_STRING)
                .putString(SCHEDULECONTAINER_TYPE, null)
                .apply()
    }

    fun getThemeMode(themeKey: String): String {
        val intValue = sharedPreferences
                .getString(themeKey, EMPTY_STRING)
                .toIntOrNull() ?: AppCompatDelegate.MODE_NIGHT_NO
        return intValue.toString()
    }
}
