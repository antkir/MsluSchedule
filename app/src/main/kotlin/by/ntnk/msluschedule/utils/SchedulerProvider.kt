package by.ntnk.msluschedule.utils

import by.ntnk.msluschedule.di.PerApp
import io.reactivex.schedulers.Schedulers
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject

@PerApp
class SchedulerProvider @Inject constructor() {
    fun ui(): Scheduler = SchedulerProvider.ui()

    fun io(): Scheduler = Schedulers.io()

    fun single(): Scheduler = Schedulers.single()

    fun newThread(): Scheduler = Schedulers.newThread()

    companion object {
        fun ui(): Scheduler = AndroidSchedulers.mainThread()
    }
}
