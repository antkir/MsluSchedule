package by.ntnk.msluschedule

import android.app.Activity
import android.app.Application
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import javax.inject.Inject
import by.ntnk.msluschedule.di.DaggerAppComponent
import timber.log.Timber

class MsluScheduleApp : Application(), HasActivityInjector {
    @Inject
    lateinit var dispatchingAndroidActivityInjector: DispatchingAndroidInjector<Activity>

    override fun onCreate() {
        super.onCreate()
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