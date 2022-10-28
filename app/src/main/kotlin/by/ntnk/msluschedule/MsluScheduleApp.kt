package by.ntnk.msluschedule

import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDexApplication
import by.ntnk.msluschedule.di.DaggerAppComponent
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import javax.inject.Inject

class MsluScheduleApp : MultiDexApplication(), HasAndroidInjector {
    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    override fun onCreate() {
        super.onCreate()

        if (!BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }

        Timber.plant(AppTimberTree())

        buildAppComponent().inject(this)

        val themeMode = sharedPreferencesRepository.getThemeMode().toInt()
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    private fun buildAppComponent(): AndroidInjector<MsluScheduleApp> {
        return DaggerAppComponent
            .factory()
            .create(this)
    }

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector
}
