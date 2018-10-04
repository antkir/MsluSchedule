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
import io.reactivex.rxkotlin.subscribeBy
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

    private var scheduleContaners: List<ScheduleContainer>? = null

    fun isTeachersNotEmpty(): Boolean = teachers != null

    fun getTeachersScheduleFilter() {
        databaseRepository.getScheduleContainers()
                .toList()
                .subscribeOn(schedulerProvider.io())
                .subscribeBy(
                        onSuccess = { scheduleContaners = it },
                        onError = { throwable -> Timber.e(throwable) }
                )

        disposable = networkRequestRepository.getTeachers()
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = {
                            teachers = it
                            populateTeachersAdapter()
                        },
                        onError = {
                            it.printStackTrace()
                            view?.showError(it)
                        }
                )
    }

    fun setTeacherValue(value: Int) {
        teacher = value
    }

    fun isValidTeacher(string: String): Boolean {
        return teachers?.containsValue(string) == true
    }

    fun isTeacherStored(string: String): Boolean {
        return scheduleContaners
                ?.filter { it.year == currentDate.academicYear }
                ?.map { it.name }
                ?.any { it == string } == true
    }

    fun getTeacher() = Teacher(teacher, teachers!!.getValue(teacher), currentDate.academicYear)

    fun populateTeachersAdapter() = view?.populateTeachersView(teachers!!)

    fun clearDisposables() = disposable.dispose()
}
