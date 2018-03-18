package by.ntnk.msluschedule.utils

import android.content.SharedPreferences
import by.ntnk.msluschedule.di.PerApp

import javax.inject.Inject

const val SCHEDULECONTAINER_ID = "scheduleContainerId"
const val SCHEDULECONTAINER_VALUE = "scheduleContainerValue"
const val SCHEDULECONTAINER_TYPE = "scheduleContainerType"

@PerApp
class SharedPreferencesRepository @Inject constructor(
        private val sharedPreferences: SharedPreferences
) {
    fun putSelectedScheduleContainer(key: Int, value: String, type: ScheduleType) {
        val typeString = ScheduleTypeConverter.scheduleTypeToString(type)
        sharedPreferences.edit()
                .putInt(SCHEDULECONTAINER_ID, key)
                .putString(SCHEDULECONTAINER_VALUE, value)
                .putString(SCHEDULECONTAINER_TYPE, typeString)
                .apply()
    }
}
