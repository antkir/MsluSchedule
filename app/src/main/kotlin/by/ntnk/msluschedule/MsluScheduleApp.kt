package by.ntnk.msluschedule

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MsluScheduleApp : MultiDexApplication() {

    @Inject
    lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    override fun onCreate() {
        super.onCreate()

        if (!BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }

        Timber.plant(AppTimberTree())

        val themeMode = sharedPreferencesRepository.getThemeMode().toInt()
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }
}
