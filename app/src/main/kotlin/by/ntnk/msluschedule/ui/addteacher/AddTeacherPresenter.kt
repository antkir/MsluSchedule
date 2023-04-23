package by.ntnk.msluschedule.ui.addteacher

import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.SchedulerProvider
import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

data class ScheduleFilterUiState(
    val isLoading: Boolean = false,
    val data: ScheduleFilter = ScheduleFilter.DEFAULT,
    val throwable: Throwable? = null
) {
    fun isData(): Boolean = data != ScheduleFilter.DEFAULT
    fun isError(): Boolean = throwable != null
}

class AddTeacherPresenter @Inject constructor(
    databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider
) : Presenter<AddTeacherView>() {

    private val scheduleContaners: BehaviorSubject<Notification<List<ScheduleContainer>>> =
        BehaviorSubject.createDefault(Notification.createOnNext(emptyList()))
    val scheduleContanersObservable: Observable<Notification<List<ScheduleContainer>>>
        get() = scheduleContaners

    private val teacherFilter: BehaviorSubject<ScheduleFilterUiState> =
        BehaviorSubject.createDefault(ScheduleFilterUiState(isLoading = true))
    val teacherFilterObservable: Observable<ScheduleFilterUiState>
        get() = teacherFilter

    private val teacherKey: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    val teacherKeyObservable: Observable<Int>
        get() = teacherKey

    init {
        databaseRepository.getScheduleContainers()
            .toList()
            .subscribeOn(schedulerProvider.io())
            .subscribeBy(
                onSuccess = { scheduleContainers ->
                    scheduleContaners.onNext(Notification.createOnNext(scheduleContainers))
                },
                onError = { throwable -> scheduleContaners.onNext(Notification.createOnError(throwable)) }
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
        assert(scheduleContaners.hasValue() && teacherFilter.hasValue())
        assert(!teacherFilter.value!!.isError() && teacherFilter.value!!.isData())
        return scheduleContaners.value?.value
            ?.filter { scheduleContainer -> scheduleContainer.year == currentDate.academicYear }
            ?.map { scheduleContainer -> scheduleContainer.name }
            ?.any { teacher -> teacher == teacherFilter.value!!.data.getValue(key) } == true
    }

    fun createSelectedTeacherObject(): Teacher {
        assert(teacherFilter.hasValue() && teacherKey.hasValue())
        assert(!teacherFilter.value!!.isError() && teacherFilter.value!!.isData())
        val teacherKey = teacherKey.value!!
        val teacherName = teacherFilter.value!!.data.getValue(teacherKey)
        return Teacher(teacherKey, teacherName, currentDate.academicYear)
    }
}
