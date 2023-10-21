package by.ntnk.msluschedule.utils

import android.os.Looper
import by.ntnk.msluschedule.di.PerApp
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors
import javax.inject.Inject

@PerApp
class SchedulerProvider @Inject constructor() {
    fun ui(): Scheduler = uiScheduler

    fun io(): Scheduler = Schedulers.io()

    fun single(): Scheduler = Schedulers.single()

    fun newSingleThreadScheduler(): Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())

    private companion object {
        private val uiScheduler: Scheduler by lazy { AndroidSchedulers.from(Looper.getMainLooper(), true) }
    }
}
