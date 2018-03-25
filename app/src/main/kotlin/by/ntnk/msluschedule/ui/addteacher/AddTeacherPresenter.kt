package by.ntnk.msluschedule.ui.addteacher

import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.singleScheduler
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import timber.log.Timber
import javax.inject.Inject

class AddTeacherPresenter @Inject constructor(
        private val networkRequestRepository: NetworkRepository,
        private val currentDate: CurrentDate
) : Presenter<AddTeacherView>() {
    private lateinit var disposable: Disposable
    private var teachers: ScheduleFilter? = null
    private var teacher: Int = 0

    val isTeachersNotEmpty: Boolean
        get() = teachers != null

    fun getTeachersScheduleFilter(uiScheduler: Scheduler) {
        disposable = networkRequestRepository.getTeachers()
                .subscribeOn(singleScheduler)
                .observeOn(uiScheduler)
                .subscribe(
                        {
                            teachers = it
                            populateTeachersAdapter()
                        },
                        { it.printStackTrace() }
                )
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

    fun clearDisposables() = disposable.dispose()
}
