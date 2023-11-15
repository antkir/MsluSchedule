package by.ntnk.msluschedule.utils

import android.os.Looper
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchedulerProvider @Inject constructor() {
    fun ui(): Scheduler = uiScheduler

    fun io(): Scheduler = Schedulers.io()

    fun single(): Scheduler = Schedulers.single()

    private companion object {
        val uiScheduler: Scheduler by lazy { AndroidSchedulers.from(Looper.getMainLooper()) }
    }
}
