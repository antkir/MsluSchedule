package by.ntnk.msluschedule

import android.app.Activity
import android.support.multidex.MultiDexApplication
import android.support.v7.app.AppCompatDelegate
import by.ntnk.msluschedule.di.DaggerAppComponent
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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        Timber.plant(Timber.DebugTree())
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