package by.ntnk.msluschedule.ui.addteacher

import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.AUTO_ID
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.SchedulerProvider
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

data class ScheduleFilterUiState(
    val isLoading: Boolean = false,
    val data: ScheduleFilter? = null,
    val throwable: Throwable? = null
) {
    fun isData(): Boolean = data != null
    fun isError(): Boolean = throwable != null
}

class AddTeacherPresenter @Inject constructor(
    databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider
) : Presenter<AddTeacherView>() {

    private val storedTeachers: BehaviorSubject<List<Teacher>> = BehaviorSubject.createDefault(emptyList())
    val storedTeachersObservable: Observable<List<Teacher>>
        get() = storedTeachers

    private val teacherFilter: BehaviorSubject<ScheduleFilterUiState> =
        BehaviorSubject.createDefault(ScheduleFilterUiState(isLoading = true))
    val teacherFilterObservable: Observable<ScheduleFilterUiState>
        get() = teacherFilter

    private val teacherKey: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    val teacherKeyObservable: Observable<Int>
        get() = teacherKey

    init {
        databaseRepository.getTeachers()
            .toList()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { teachers -> storedTeachers.onNext(teachers) },
                onError = { throwable -> storedTeachers.onError(throwable) }
            )

        getTeacherScheduleFilter()
    }

    private fun getTeacherScheduleFilter() {
        teacherFilter.onNext(ScheduleFilterUiState(isLoading = true))
        networkRepository.getTeachers()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter -> teacherFilter.onNext(ScheduleFilterUiState(data = scheduleFilter)) },
                onError = { throwable -> teacherFilter.onNext(ScheduleFilterUiState(throwable = throwable)) }
            )
    }

    fun setTeacherKey(key: Int) {
        teacherKey.onNext(key)
    }

    fun isTeacherAdded(key: Int): Boolean {
        assert(storedTeachers.hasValue() && teacherFilter.hasValue())
        assert(!teacherFilter.value!!.isError() && teacherFilter.value!!.isData())
        return storedTeachers.value
            ?.filter { teacher -> teacher.year == currentDate.academicYear }
            ?.map { teacher -> teacher.value }
            ?.any { teacherName -> teacherName == teacherFilter.value!!.data!!.getValue(key) } == true
    }

    fun createSelectedTeacherObject(): Teacher {
        assert(teacherFilter.hasValue() && teacherKey.hasValue())
        assert(!teacherFilter.value!!.isError() && teacherFilter.value!!.isData())
        val teacherKey = teacherKey.value!!
        val teacherName = teacherFilter.value!!.data!!.getValue(teacherKey)
        return Teacher(AUTO_ID, teacherKey, teacherName, currentDate.academicYear)
    }
}
