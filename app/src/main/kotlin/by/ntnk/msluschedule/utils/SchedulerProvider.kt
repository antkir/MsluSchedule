package by.ntnk.msluschedule.utils

import android.os.Looper
import by.ntnk.msluschedule.di.PerApp
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

@PerApp
class SchedulerProvider @Inject constructor() {
    fun ui(): Scheduler = uiScheduler

    fun io(): Scheduler = Schedulers.io()

    fun single(): Scheduler = Schedulers.single()

    private companion object {
        val uiScheduler: Scheduler = AndroidSchedulers.from(Looper.getMainLooper(), true)
    }
}
