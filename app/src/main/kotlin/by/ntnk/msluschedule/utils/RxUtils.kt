package by.ntnk.msluschedule.utils

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

val uiScheduler: Scheduler = AndroidSchedulers.mainThread()
val singleScheduler = Schedulers.single()
val ioScheduler = Schedulers.io()

operator fun CompositeDisposable.plusAssign(disposable: Disposable) {
    add(disposable)
}
