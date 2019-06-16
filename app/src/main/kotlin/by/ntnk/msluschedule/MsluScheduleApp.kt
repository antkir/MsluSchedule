package by.ntnk.msluschedule

import android.app.Activity
import androidx.multidex.MultiDexApplication
import by.ntnk.msluschedule.di.DaggerAppComponent
import com.crashlytics.android.Crashlytics
import com.squareup.leakcanary.LeakCanary
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasActivityInjector
import io.fabric.sdk.android.Fabric
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

        if (!BuildConfig.DEBUG) {
            val fabric = Fabric.Builder(this).kits(Crashlytics()).build()
            Fabric.with(fabric)
        }

        Timber.plant(AppTimberTree())

        buildAppComponent().inject(this)
    }

    private fun buildAppComponent(): AndroidInjector<MsluScheduleApp> {
        return DaggerAppComponent
                .factory()
                .create(this)
    }

    override fun activityInjector(): AndroidInjector<Activity> {
        return dispatchingAndroidActivityInjector
    }
}
