package by.ntnk.msluschedule.ui.addteacher

import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CurrentDate
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import javax.inject.Inject

class AddTeacherPresenter @Inject constructor(
        private val networkRequestRepository: NetworkRepository,
        private val currentDate: CurrentDate
) : Presenter<AddTeacherView>() {
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var teachers: ScheduleFilter? = null
    private var teacher: Int = 0

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

    fun setTeacherValue(value: Int) {
        Timber.d("VALUE: %d", value)
        teacher = value
    }

    fun isValidTeacher(string: String): Boolean {
        return teachers?.containsValue(string) ?: false
    }

    fun getTeacher(): Teacher =
            Teacher(teacher, teachers!!.getValue(teacher), currentDate.academicYear)

    fun populateTeachersAdapter() = view!!.populateTeachersView(teachers!!)

    fun clearDisposables() = disposables.clear()
}
