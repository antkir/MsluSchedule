package by.ntnk.msluschedule.ui.addgroup

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.AUTO_ID
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.NetworkApiVersion
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

data class StoredGroupsLoadedEvent(
    private var isLoaded: Boolean = false,
    private var throwable: Throwable? = null
) {
    fun isEvent(): Boolean = isLoaded

    fun consumeEvent() {
        isLoaded = false
    }

    fun isError(): Boolean = throwable != null

    fun consumeError(): Throwable {
        val throwableCopy = throwable!!
        throwable = null
        return throwableCopy
    }
}

data class ScheduleFilterUiState(
    val isLoading: Boolean = false,
    val data: ScheduleFilter? = null,
    val throwable: Throwable? = null
) {
    fun isData(): Boolean = data != null
    fun isError(): Boolean = throwable != null
}

class AddGroupPresenter @Inject constructor(
    databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider,
    private val sharedPreferencesRepository: SharedPreferencesRepository
) : Presenter<AddGroupView>() {

    private val storedGroups: BehaviorSubject<List<StudyGroup>> = BehaviorSubject.createDefault(emptyList())

    private val storedGroupsLoadedEvent: BehaviorSubject<StoredGroupsLoadedEvent> =
        BehaviorSubject.createDefault(StoredGroupsLoadedEvent())
    val storedGroupsLoadedEventObservable: Observable<StoredGroupsLoadedEvent>
        get() = storedGroupsLoadedEvent

    private val facultyFilter: BehaviorSubject<ScheduleFilterUiState> =
        BehaviorSubject.createDefault(ScheduleFilterUiState())
    val facultyFilterObservable: Observable<ScheduleFilterUiState>
        get() = facultyFilter

    private val facultyKey: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    val facultyKeyObservable: Observable<Int>
        get() = facultyKey

    private val courseFilter: BehaviorSubject<ScheduleFilterUiState> =
        BehaviorSubject.createDefault(ScheduleFilterUiState())
    val courseFilterObservable: Observable<ScheduleFilterUiState>
        get() = courseFilter

    private val courseKey: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    val courseKeyObservable: Observable<Int>
        get() = courseKey

    private val groupFilter: BehaviorSubject<ScheduleFilterUiState> =
        BehaviorSubject.createDefault(ScheduleFilterUiState())
    val groupFilterObservable: Observable<ScheduleFilterUiState>
        get() = groupFilter

    private val groupKey: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)
    val groupKeyObservable: Observable<Int>
        get() = groupKey

    private var courseFilterDisposable: Disposable = CompositeDisposable()
    private var groupFilterDisposable: Disposable = CompositeDisposable()

    init {
        databaseRepository.getStudyGroups()
            .toList()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { groups ->
                    storedGroups.onNext(groups)
                    storedGroupsLoadedEvent.onNext(StoredGroupsLoadedEvent(isLoaded = true))
                },
                onError = { throwable ->
                    storedGroupsLoadedEvent.onNext(StoredGroupsLoadedEvent(throwable = throwable))
                }
            )

        downloadFacultyFilter()
    }

    fun isCourseFilterUiStateDefault(): Boolean {
        return courseFilter.value == ScheduleFilterUiState()
    }

    fun isGroupFilterUiStateDefault(): Boolean {
        return groupFilter.value == ScheduleFilterUiState()
    }

    fun isGroupKeyDefault(): Boolean {
        return groupKey.value == Int.DEFAULT
    }

    fun setFacultyKeyFromPosition(position: Int) {
        assert(position >= 0)
        assert(facultyFilter.hasValue())
        assert(!facultyFilter.value!!.isError() && facultyFilter.value!!.isData())
        facultyKey.onNext(facultyFilter.value!!.data!!.keyAt(position))
    }

    fun setCourseKeyFromPosition(position: Int) {
        assert(position >= 0)
        assert(courseFilter.hasValue())
        assert(!courseFilter.value!!.isError() && courseFilter.value!!.isData())
        courseKey.onNext(courseFilter.value!!.data!!.keyAt(position))
    }

    fun setGroupKey(key: Int) {
        groupKey.onNext(key)
    }

    fun resetCourses() {
        courseFilterDisposable.dispose()
        courseFilter.onNext(ScheduleFilterUiState())
        courseKey.onNext(Int.DEFAULT)
    }

    fun resetGroups() {
        groupFilterDisposable.dispose()
        groupFilter.onNext(ScheduleFilterUiState())
        groupKey.onNext(Int.DEFAULT)
    }

    private fun downloadFacultyFilter() {
        facultyFilter.onNext(ScheduleFilterUiState(isLoading = true))
        networkRepository.getFaculties()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter -> facultyFilter.onNext(ScheduleFilterUiState(data = scheduleFilter)) },
                onError = { throwable -> facultyFilter.onNext(ScheduleFilterUiState(throwable = throwable)) }
            )
    }

    fun downloadCourseFilter() {
        assert(facultyKey.hasValue())
        courseFilter.onNext(ScheduleFilterUiState(isLoading = true))
        courseFilterDisposable = networkRepository.getCourses(facultyKey.value!!)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter -> courseFilter.onNext(ScheduleFilterUiState(data = scheduleFilter)) },
                onError = { throwable -> courseFilter.onNext(ScheduleFilterUiState(throwable = throwable)) }
            )
    }

    fun downloadGroupFilter() {
        assert(facultyKey.hasValue() && courseKey.hasValue())
        groupFilter.onNext(ScheduleFilterUiState(isLoading = true))
        groupFilterDisposable =
            networkRepository.getGroups(facultyKey.value!!, courseKey.value!!, currentDate.academicYear)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                    onSuccess = { scheduleFilter -> groupFilter.onNext(ScheduleFilterUiState(data = scheduleFilter)) },
                    onError = { throwable -> groupFilter.onNext(ScheduleFilterUiState(throwable = throwable)) }
                )
    }

    fun isGroupAdded(key: Int): Boolean {
        assert(storedGroups.hasValue() && facultyKey.hasValue() && groupFilter.hasValue())
        assert(!groupFilter.value!!.isError() && groupFilter.value!!.isData())
        return storedGroups.value
            ?.filter { group -> group.year == currentDate.academicYear }
            ?.filter { group -> group.faculty == facultyKey.value }
            ?.any { group ->
                val facultyKey = facultyKey.value!!
                val facultyName = facultyFilter.value!!.data!!.getValue(facultyKey)
                val groupName = groupFilter.value!!.data!!.getValue(key)
                group.value == formatGroupName(groupName, facultyName)
            } == true
    }

    fun createSelectedStudyGroupObject(): StudyGroup {
        assert(facultyFilter.hasValue() && facultyKey.hasValue())
        assert(courseFilter.hasValue() && courseKey.hasValue())
        assert(groupFilter.hasValue() && groupKey.hasValue())
        assert(!facultyFilter.value!!.isError() && facultyFilter.value!!.isData())
        assert(!groupFilter.value!!.isError() && groupFilter.value!!.isData())
        val facultyKey = facultyKey.value!!
        val facultyName = facultyFilter.value!!.data!!.getValue(facultyKey)
        val groupKey = groupKey.value!!
        val groupName = groupFilter.value!!.data!!.getValue(groupKey)
        val name = formatGroupName(groupName, facultyName)
        return StudyGroup(AUTO_ID, groupKey, name, currentDate.academicYear, facultyKey, courseKey.value!!)
    }

    private fun formatGroupName(groupName: String, facultyName: String): String {
        return if (sharedPreferencesRepository.getCurrentNetworkApiVersion() == NetworkApiVersion.MYUNIVERSITY) {
            "$groupName ($facultyName)"
        } else {
            groupName
        }
    }
}
