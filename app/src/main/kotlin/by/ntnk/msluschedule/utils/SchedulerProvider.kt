package by.ntnk.msluschedule.utils

import android.os.Looper
import by.ntnk.msluschedule.di.PerApp
import io.reactivex.schedulers.Schedulers
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.Executors
import javax.inject.Inject

@PerApp
class SchedulerProvider @Inject constructor() {
    fun ui(): Scheduler = SchedulerProvider.ui()

    fun io(): Scheduler = Schedulers.io()

    fun single(): Scheduler = Schedulers.single()

    fun cachedThreadPool(): Scheduler = Schedulers.from(Executors.newCachedThreadPool())

    companion object {
        fun ui(): Scheduler = AndroidSchedulers.from(Looper.getMainLooper(), true)
    }
}
