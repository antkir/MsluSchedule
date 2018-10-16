package by.ntnk.msluschedule.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.di.PerApp

import javax.inject.Inject

const val SCHEDULECONTAINER_ID = "scheduleContainerId"
const val SCHEDULECONTAINER_VALUE = "scheduleContainerValue"
const val SCHEDULECONTAINER_TYPE = "scheduleContainerType"

@PerApp
class SharedPreferencesRepository @Inject constructor(
        private val applicationContext: Context, private val sharedPreferences: SharedPreferences) {
    fun getSelectedScheduleContainerInfo(): ScheduleContainerInfo {
        val id = sharedPreferences.getInt(SCHEDULECONTAINER_ID, 0)
        val value = sharedPreferences.getString(SCHEDULECONTAINER_VALUE, EMPTY_STRING) ?: EMPTY_STRING
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

    fun getThemeMode(): String {
        val intValue = sharedPreferences
                .getString(applicationContext.getString(R.string.key_theme), EMPTY_STRING)
                ?.toIntOrNull() ?: AppCompatDelegate.MODE_NIGHT_NO
        return intValue.toString()
    }

    fun isMainFabShown(): Boolean {
        return sharedPreferences
                .getBoolean(applicationContext.getString(R.string.key_show_add_schedule), true)
    }

    fun isFullSubjectNameUsed(): Boolean {
        return sharedPreferences
                .getBoolean(applicationContext.getString(R.string.key_full_subjects), false)
    }

    fun isPhysEdClassHidden(): Boolean {
        return sharedPreferences
                .getBoolean(applicationContext.getString(R.string.key_hide_pe_classes), false)
    }
}
