package by.ntnk.msluschedule.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.di.PerApp
import javax.inject.Inject

@PerApp
class SharedPreferencesRepository @Inject constructor(
    private val applicationContext: Context,
    private val sharedPreferences: SharedPreferences
) {
    var isFirstAppLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_APP_LAUNCH, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_FIRST_APP_LAUNCH, value).apply()

    fun getSelectedScheduleContainer(): ScheduleContainerInfo {
        val id = sharedPreferences.getInt(KEY_SCHEDULECONTAINER_ID, 0)
        val value = sharedPreferences.getString(KEY_SCHEDULECONTAINER_VALUE, null) ?: String.DEFAULT
        val typeString = sharedPreferences.getString(KEY_SCHEDULECONTAINER_TYPE, null) ?: String.DEFAULT
        val type = ScheduleTypeConverter.stringToScheduleType(typeString)
        return ScheduleContainerInfo(id, value, type)
    }

    fun putSelectedScheduleContainer(scheduleContainer: ScheduleContainerInfo) {
        sharedPreferences.edit()
            .putInt(KEY_SCHEDULECONTAINER_ID, scheduleContainer.id)
            .putString(KEY_SCHEDULECONTAINER_VALUE, scheduleContainer.value)
            .putString(KEY_SCHEDULECONTAINER_TYPE, scheduleContainer.type.name)
            .apply()
    }

    fun selectEmptyScheduleContainer() {
        sharedPreferences.edit()
            .putInt(KEY_SCHEDULECONTAINER_ID, 0)
            .putString(KEY_SCHEDULECONTAINER_VALUE, String.DEFAULT)
            .putString(KEY_SCHEDULECONTAINER_TYPE, ScheduleType.DEFAULT.name)
            .apply()
    }

    fun getThemeMode(): String {
        val intValue = sharedPreferences.getString(applicationContext.getString(R.string.key_theme), String.DEFAULT)?.toIntOrNull()
            ?: AppCompatDelegate.MODE_NIGHT_NO
        return intValue.toString()
    }

    fun isMainFabShown(): Boolean {
        return sharedPreferences.getBoolean(applicationContext.getString(R.string.key_show_add_schedule), true)
    }

    fun isFullSubjectNameUsed(): Boolean {
        return sharedPreferences.getBoolean(applicationContext.getString(R.string.key_full_subjects), false)
    }

    fun isPhysEdClassHidden(): Boolean {
        return sharedPreferences.getBoolean(applicationContext.getString(R.string.key_hide_pe_classes), false)
    }

    private companion object {
        const val KEY_SCHEDULECONTAINER_ID = "scheduleContainerId"
        const val KEY_SCHEDULECONTAINER_VALUE = "scheduleContainerValue"
        const val KEY_SCHEDULECONTAINER_TYPE = "scheduleContainerType"
        const val KEY_FIRST_APP_LAUNCH = "isFirstAppLaunch"
    }
}
