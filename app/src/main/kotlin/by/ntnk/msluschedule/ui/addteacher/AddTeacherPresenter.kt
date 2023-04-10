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
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

class AddTeacherPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider
) : Presenter<AddTeacherView>() {

    private val disposables: CompositeDisposable = CompositeDisposable()

    private val scheduleContaners: BehaviorSubject<Notification<List<ScheduleContainer>>> =
        BehaviorSubject.createDefault(Notification.createOnNext(emptyList()))
    private val teacherFilter: BehaviorSubject<Notification<ScheduleFilter>> =
        BehaviorSubject.createDefault(Notification.createOnNext(ScheduleFilter.DEFAULT))
    private val teacherKey: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    val scheduleContanersObservable: Observable<Notification<List<ScheduleContainer>>>
        get() = scheduleContaners
    val teacherFilterObservable: Observable<Notification<ScheduleFilter>>
        get() = teacherFilter
    val teacherKeyObservable: Observable<Int>
        get() = teacherKey

    fun isTeacherFilterDefault(): Boolean {
        return teacherFilter.value?.value == ScheduleFilter.DEFAULT
    }

    fun getTeacherScheduleFilter() {
        disposables += databaseRepository.getScheduleContainers()
            .toList()
            .subscribeOn(schedulerProvider.io())
            .subscribeBy(
                onSuccess = { scheduleContainers ->
                    scheduleContaners.onNext(Notification.createOnNext(scheduleContainers))
                },
                onError = { throwable -> scheduleContaners.onNext(Notification.createOnError(throwable)) }
            )

        disposables += networkRepository.getTeachers()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter -> teacherFilter.onNext(Notification.createOnNext(scheduleFilter)) },
                onError = { throwable -> teacherFilter.onNext(Notification.createOnError(throwable)) }
            )
    }

    fun setTeacherKey(key: Int) {
        teacherKey.onNext(key)
    }

    fun isTeacherAdded(key: Int): Boolean {
        assert(scheduleContaners.hasValue() && teacherFilter.hasValue())
        assert(teacherFilter.value!!.value != null)
        return scheduleContaners.value?.value
            ?.filter { scheduleContainer -> scheduleContainer.year == currentDate.academicYear }
            ?.map { scheduleContainer -> scheduleContainer.name }
            ?.any { teacher -> teacher == teacherFilter.value!!.value!!.getValue(key) } == true
    }

    fun createSelectedTeacherObject(): Teacher {
        assert(teacherFilter.hasValue() && teacherKey.hasValue())
        assert(teacherFilter.value!!.value != null)
        val teacherKey = teacherKey.value!!
        val teacherName = teacherFilter.value!!.value!!.getValue(teacherKey)
        return Teacher(teacherKey, teacherName, currentDate.academicYear)
    }

    fun clearDisposables() = disposables.clear()
}
