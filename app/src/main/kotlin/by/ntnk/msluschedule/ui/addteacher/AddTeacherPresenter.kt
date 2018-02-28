package by.ntnk.msluschedule.ui.addteacher

import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class AddTeacherPresenter @Inject
constructor(private val networkRequestRepository: NetworkRepository) : Presenter<AddTeacherView>() {
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var teachers: ScheduleFilter? = null

    val isTeachersNotEmpty: Boolean
        get() = teachers != null

    fun getTeachersScheduleFilter(uiScheduler: Scheduler) {
        val disposable = networkRequestRepository.getTeachers()
                .observeOn(uiScheduler)
                .subscribe(
                        {
                            teachers = it
                            populateTeachersAdapter()
                        },
                        { it.printStackTrace() }
                )
        disposables.add(disposable)
    }

    fun populateTeachersAdapter() = view!!.populateTeachersView(teachers!!)

    fun clearDisposables() = disposables.clear()
}
