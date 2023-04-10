package by.ntnk.msluschedule.ui.addgroup

import by.ntnk.msluschedule.data.StudyGroup
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

class AddGroupPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider
) : Presenter<AddGroupView>() {

    private val disposables: CompositeDisposable = CompositeDisposable()

    private val scheduleContaners: BehaviorSubject<Notification<List<ScheduleContainer>>> =
        BehaviorSubject.createDefault(Notification.createOnNext(emptyList()))
    private val facultyFilter: BehaviorSubject<Notification<ScheduleFilter>> =
        BehaviorSubject.createDefault(Notification.createOnNext(ScheduleFilter.DEFAULT))
    private val facultyKey: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    private val courseFilter: BehaviorSubject<Notification<ScheduleFilter>> =
        BehaviorSubject.createDefault(Notification.createOnNext(ScheduleFilter.DEFAULT))
    private val courseKey: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    private val groupFilter: BehaviorSubject<Notification<ScheduleFilter>> =
        BehaviorSubject.createDefault(Notification.createOnNext(ScheduleFilter.DEFAULT))
    private val groupKey: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    val scheduleContanersObservable: Observable<Notification<List<ScheduleContainer>>>
        get() = scheduleContaners
    val facultyFilterObservable: Observable<Notification<ScheduleFilter>>
        get() = facultyFilter
    val facultyKeyObservable: Observable<Int>
        get() = facultyKey
    val courseFilterObservable: Observable<Notification<ScheduleFilter>>
        get() = courseFilter
    val courseKeyObservable: Observable<Int>
        get() = courseKey
    val groupFilterObservable: Observable<Notification<ScheduleFilter>>
        get() = groupFilter
    val groupKeyObservable: Observable<Int>
        get() = groupKey

    fun isFacultyFilterDefault(): Boolean {
        return facultyFilter.value?.value == ScheduleFilter.DEFAULT
    }

    fun isFacultyKeyDefault(): Boolean {
        return facultyKey.value == Int.DEFAULT
    }

    fun isCourseFilterDefault(): Boolean {
        return courseFilter.value?.value == ScheduleFilter.DEFAULT
    }

    fun isCourseKeyDefault(): Boolean {
        return courseKey.value == Int.DEFAULT
    }

    fun isGroupFilterDefault(): Boolean {
        return groupFilter.value?.value == ScheduleFilter.DEFAULT
    }

    fun isGroupKeyDefault(): Boolean {
        return groupKey.value == Int.DEFAULT
    }

    fun setFacultyKeyFromPosition(position: Int) {
        assert(position >= 0)
        assert(facultyFilter.hasValue())
        assert(facultyFilter.value!!.value != null)
        facultyKey.onNext(facultyFilter.value!!.value!!.keyAt(position))
    }

    fun setCourseKeyFromPosition(position: Int) {
        assert(position >= 0)
        assert(courseFilter.hasValue())
        assert(courseFilter.value!!.value != null)
        courseKey.onNext(courseFilter.value!!.value!!.keyAt(position))
    }

    fun setGroupKey(key: Int) {
        groupKey.onNext(key)
    }

    fun resetCourses() {
        courseFilter.onNext(Notification.createOnNext(ScheduleFilter.DEFAULT))
        courseKey.onNext(Int.DEFAULT)
    }

    fun resetGroups() {
        groupFilter.onNext(Notification.createOnNext(ScheduleFilter.DEFAULT))
        groupKey.onNext(Int.DEFAULT)
    }

    fun getFacultyScheduleFilter() {
        disposables += databaseRepository.getScheduleContainers()
            .toList()
            .subscribeOn(schedulerProvider.io())
            .subscribeBy(
                onSuccess = { scheduleContainers ->
                    scheduleContaners.onNext(Notification.createOnNext(scheduleContainers))
                },
                onError = { throwable -> scheduleContaners.onNext(Notification.createOnError(throwable)) }
            )

        disposables += networkRepository.getFaculties()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter -> facultyFilter.onNext(Notification.createOnNext(scheduleFilter)) },
                onError = { throwable -> facultyFilter.onNext(Notification.createOnError(throwable)) }
            )
    }

    fun getCourseScheduleFilter() {
        assert(facultyKey.hasValue())
        disposables += networkRepository.getCourses(facultyKey.value!!)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter -> courseFilter.onNext(Notification.createOnNext(scheduleFilter)) },
                onError = { throwable -> courseFilter.onNext(Notification.createOnError(throwable)) }
            )
    }

    fun getGroupScheduleFilter() {
        assert(facultyKey.hasValue() && courseKey.hasValue())
        disposables += networkRepository.getGroups(facultyKey.value!!, courseKey.value!!, currentDate.academicYear)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter -> groupFilter.onNext(Notification.createOnNext(scheduleFilter)) },
                onError = { throwable -> groupFilter.onNext(Notification.createOnError(throwable)) }
            )
    }

    fun isGroupAdded(key: Int): Boolean {
        assert(scheduleContaners.hasValue() && facultyKey.hasValue() && groupFilter.hasValue())
        assert(groupFilter.value!!.value != null)
        return scheduleContaners.value?.value
            ?.filter { scheduleContainer -> scheduleContainer.year == currentDate.academicYear }
            ?.filter { scheduleContainer -> scheduleContainer.faculty == facultyKey.value }
            ?.map { scheduleContainer -> scheduleContainer.name }
            ?.any { group -> group == groupFilter.value!!.value!!.getValue(key) } == true
    }

    fun createSelectedStudyGroupObject(): StudyGroup {
        assert(facultyFilter.hasValue() && facultyKey.hasValue())
        assert(courseFilter.hasValue() && courseKey.hasValue())
        assert(groupFilter.hasValue() && groupKey.hasValue())
        assert(groupFilter.value!!.value != null)
        val groupKey = groupKey.value!!
        val groupName = groupFilter.value!!.value!!.getValue(groupKey)
        return StudyGroup(groupKey, groupName, facultyKey.value!!, courseKey.value!!, currentDate.academicYear)
    }

    fun clearDisposables() = disposables.clear()
}
