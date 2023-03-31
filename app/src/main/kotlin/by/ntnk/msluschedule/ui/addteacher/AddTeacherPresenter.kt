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
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import javax.inject.Inject

class AddTeacherPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider
) : Presenter<AddTeacherView>() {

    private lateinit var disposable: Disposable

    private val scheduleContaners: BehaviorSubject<List<ScheduleContainer>> = BehaviorSubject.createDefault(emptyList())
    private val teacherFilter: BehaviorSubject<ScheduleFilter> = BehaviorSubject.createDefault(ScheduleFilter.DEFAULT)
    private val teacherId: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    val teacherFilterObservable: Observable<ScheduleFilter>
        get() = teacherFilter
    val teacherIdObservable: Observable<Int>
        get() = teacherId

    fun isTeacherFilterDefault(): Boolean {
        return teacherFilter.value == ScheduleFilter.DEFAULT
    }

    fun getTeacherScheduleFilter() {
        databaseRepository.getScheduleContainers()
            .toList()
            .subscribeOn(schedulerProvider.io())
            .subscribeBy(
                onSuccess = { scheduleContainers -> scheduleContaners.onNext(scheduleContainers) },
                onError = { throwable -> Timber.e(throwable) }
            )

        disposable = networkRepository.getTeachers()
            .subscribeOn(schedulerProvider.single())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter -> teacherFilter.onNext(scheduleFilter) },
                onError = { throwable -> teacherFilter.onError(throwable) }
            )
    }

    fun setTeacherId(id: Int) {
        teacherId.onNext(id)
    }

    fun isTeacherAdded(id: Int): Boolean {
        assert(scheduleContaners.hasValue() && teacherFilter.hasValue())
        return scheduleContaners.value
            ?.filter { scheduleContainer -> scheduleContainer.year == currentDate.academicYear }
            ?.map { scheduleContainer -> scheduleContainer.name }
            ?.any { teacher -> teacher == teacherFilter.value!!.getValue(id) } == true
    }

    fun createSelectedTeacherObject(): Teacher {
        assert(teacherFilter.hasValue() && teacherId.hasValue())
        val teacherId = teacherId.value!!
        return Teacher(teacherId, teacherFilter.value!!.getValue(teacherId), currentDate.academicYear)
    }

    fun clearDisposables() = disposable.dispose()
}
