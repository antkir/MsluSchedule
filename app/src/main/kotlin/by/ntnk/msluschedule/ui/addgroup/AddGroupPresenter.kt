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
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import javax.inject.Inject

class AddGroupPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider
) : Presenter<AddGroupView>() {

    private val disposables: CompositeDisposable = CompositeDisposable()

    private val scheduleContaners: BehaviorSubject<List<ScheduleContainer>> = BehaviorSubject.createDefault(emptyList())
    private val facultyFilter: BehaviorSubject<ScheduleFilter> = BehaviorSubject.createDefault(ScheduleFilter.DEFAULT)
    private val facultyId: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    private val courseFilter: BehaviorSubject<ScheduleFilter> = BehaviorSubject.createDefault(ScheduleFilter.DEFAULT)
    private val courseId: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    private val groupFilter: BehaviorSubject<ScheduleFilter> = BehaviorSubject.createDefault(ScheduleFilter.DEFAULT)
    private val groupId: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    val facultyFilterObservable: Observable<ScheduleFilter>
        get() = facultyFilter
    val facultyIdObservable: Observable<Int>
        get() = facultyId
    val courseFilterObservable: Observable<ScheduleFilter>
        get() = courseFilter
    val courseIdObservable: Observable<Int>
        get() = courseId
    val groupFilterObservable: Observable<ScheduleFilter>
        get() = groupFilter
    val groupIdObservable: Observable<Int>
        get() = groupId

    fun isFacultyFilterDefault(): Boolean {
        return facultyFilter.value == ScheduleFilter.DEFAULT
    }

    fun isFacultyIdDefault(): Boolean {
        return facultyId.value == Int.DEFAULT
    }

    fun isCourseFilterDefault(): Boolean {
        return courseFilter.value == ScheduleFilter.DEFAULT
    }

    fun isCourseIdDefault(): Boolean {
        return courseId.value == Int.DEFAULT
    }

    fun isGroupFilterDefault(): Boolean {
        return groupFilter.value == ScheduleFilter.DEFAULT
    }

    fun isGroupIdDefault(): Boolean {
        return groupId.value == Int.DEFAULT
    }

    fun setFacultyIdFromPosition(position: Int) {
        assert(position >= 0)
        assert(facultyFilter.hasValue())
        facultyId.onNext(facultyFilter.value!!.keyAt(position))
    }

    fun setCourseIdFromPosition(position: Int) {
        assert(position >= 0)
        assert(courseFilter.hasValue())
        courseId.onNext(courseFilter.value!!.keyAt(position))
    }

    fun setGroupId(id: Int) {
        groupId.onNext(id)
    }

    fun resetCourses() {
        courseFilter.onNext(ScheduleFilter.DEFAULT)
        courseId.onNext(Int.DEFAULT)
    }

    fun resetGroups() {
        groupFilter.onNext(ScheduleFilter.DEFAULT)
        groupId.onNext(Int.DEFAULT)
    }

    fun getFacultyScheduleFilter() {
        databaseRepository.getScheduleContainers()
            .toList()
            .subscribeOn(schedulerProvider.io())
            .subscribeBy(
                onSuccess = { scheduleContainers -> scheduleContaners.onNext(scheduleContainers) },
                onError = { throwable -> Timber.e(throwable) }
            )

        disposables += networkRepository.getFaculties()
            .subscribeOn(schedulerProvider.single())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter -> facultyFilter.onNext(scheduleFilter) },
                onError = { throwable -> facultyFilter.onError(throwable) }
            )
    }

    fun getCourseScheduleFilter() {
        disposables += networkRepository.getCourses()
            .subscribeOn(schedulerProvider.single())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter -> courseFilter.onNext(scheduleFilter) },
                onError = { throwable -> courseFilter.onError(throwable) }
            )
    }

    fun getGroupScheduleFilter() {
        assert(facultyId.hasValue() && courseId.hasValue())
        disposables += networkRepository.getGroups(facultyId.value!!, courseId.value!!, currentDate.academicYear)
            .subscribeOn(schedulerProvider.single())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter -> groupFilter.onNext(scheduleFilter) },
                onError = { throwable -> groupFilter.onError(throwable) }
            )
    }

    fun isGroupAdded(id: Int): Boolean {
        assert(scheduleContaners.hasValue() && facultyId.hasValue() && groupFilter.hasValue())
        return scheduleContaners.value
            ?.filter { scheduleContainer -> scheduleContainer.year == currentDate.academicYear }
            ?.filter { scheduleContainer -> scheduleContainer.faculty == facultyId.value }
            ?.map { scheduleContainer -> scheduleContainer.name }
            ?.any { group -> group == groupFilter.value!!.getValue(id) } == true
    }

    fun createSelectedStudyGroupObject(): StudyGroup {
        assert(facultyFilter.hasValue() && facultyId.hasValue())
        assert(courseFilter.hasValue() && courseId.hasValue())
        assert(groupFilter.hasValue() && groupId.hasValue())
        val groupId = groupId.value!!
        return StudyGroup(groupId, groupFilter.value!!.getValue(groupId), facultyId.value!!, courseId.value!!, currentDate.academicYear)
    }

    fun clearDisposables() = disposables.clear()
}
