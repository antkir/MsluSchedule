package by.ntnk.msluschedule.ui.addteacher

import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.SchedulerProvider
import io.reactivex.disposables.Disposable
import timber.log.Timber
import javax.inject.Inject

class AddTeacherPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository,
        private val networkRequestRepository: NetworkRepository,
        private val currentDate: CurrentDate,
        private val schedulerProvider: SchedulerProvider
) : Presenter<AddTeacherView>() {
    private lateinit var disposable: Disposable
    private var teachers: ScheduleFilter? = null
    private var teacher: Int = 0

    private lateinit var scheduleContaners: List<ScheduleContainer>

    val isTeachersNotEmpty: Boolean
        get() = teachers != null

    fun getTeachersScheduleFilter() {
        databaseRepository.getScheduleContainers()
                .toList()
                .subscribeOn(schedulerProvider.io())
                .subscribe(
                        { scheduleContaners = it },
                        { it.printStackTrace() }
                )

        disposable = networkRequestRepository.getTeachers()
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
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
        return teachers?.containsValue(string) == true
    }

    fun isTeacherStored(string: String): Boolean {
        return scheduleContaners
                .filter { it.year == currentDate.academicYear }
                .map { it.name }
                .any { it == string }
    }

    fun getTeacher() = Teacher(teacher, teachers!!.getValue(teacher), currentDate.academicYear)

    fun populateTeachersAdapter() = view!!.populateTeachersView(teachers!!)

    fun clearDisposables() = disposable.dispose()
}
