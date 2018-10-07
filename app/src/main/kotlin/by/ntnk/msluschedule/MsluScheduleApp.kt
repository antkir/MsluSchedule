package by.ntnk.msluschedule

import android.app.Activity
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import android.support.v7.preference.PreferenceManager
import by.ntnk.msluschedule.di.DaggerAppComponent
import by.ntnk.msluschedule.utils.EMPTY_STRING
import com.squareup.leakcanary.LeakCanary
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import timber.log.Timber
import javax.inject.Inject

class MsluScheduleApp : MultiDexApplication(), HasActivityInjector {
    @Inject
    lateinit var dispatchingAndroidActivityInjector: DispatchingAndroidInjector<Activity>

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return
        }
        LeakCanary.install(this)

        val themeMode = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(getString(R.string.key_theme), EMPTY_STRING)
                .toIntOrNull() ?: AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(themeMode)
        Timber.plant(AppTimberTree())

        buildAppComponent().inject(this)
    }

    private fun buildAppComponent(): AndroidInjector<MsluScheduleApp> {
        return DaggerAppComponent
                .builder()
                .create(this)
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return dispatchingAndroidActivityInjector
    }
}
