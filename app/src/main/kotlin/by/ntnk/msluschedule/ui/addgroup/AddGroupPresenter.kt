package by.ntnk.msluschedule.ui.addgroup

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.AUTO_ID
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.ErrorType
import by.ntnk.msluschedule.utils.NetworkApiVersion
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import by.ntnk.msluschedule.utils.Utils
import by.ntnk.msluschedule.utils.assertDebug
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import javax.inject.Inject

data class DataState(
    val data: ScheduleFilter? = null
)

data class ScheduleFilterUiState(
    val isLoading: Boolean = false,
    val dataState: DataState = DataState(),
    val key: String = String.DEFAULT
)

data class UiEvent(
    val isTriggered: Boolean = false
)

data class ErrorUiEvent(
    val error: ErrorType? = null
)

data class AddGroupUiState(
    val facultyFilterUiState: ScheduleFilterUiState = ScheduleFilterUiState(),
    val courseFilterUiState: ScheduleFilterUiState = ScheduleFilterUiState(),
    val groupFilterUiState: ScheduleFilterUiState = ScheduleFilterUiState(),
    val storedGroupsLoadedEvent: UiEvent = UiEvent(),
    val errorEvent: ErrorUiEvent = ErrorUiEvent()
)

class AddGroupPresenter @Inject constructor(
    databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider,
    private val sharedPreferencesRepository: SharedPreferencesRepository
) : Presenter<AddGroupView>() {

    private val storedGroups: BehaviorSubject<List<StudyGroup>> = BehaviorSubject.createDefault(emptyList())

    private val uiState: BehaviorSubject<AddGroupUiState> = BehaviorSubject.createDefault(AddGroupUiState())
    val uiStateObservable: Observable<AddGroupUiState>
        get() = uiState

    private val disposables: CompositeDisposable = CompositeDisposable()
    private var courseNetworkRequestDisposable: Disposable = CompositeDisposable()
    private var groupsNetworkRequestDisposable: Disposable = CompositeDisposable()

    init {
        disposables += databaseRepository.getStudyGroups()
            .toList()
            .doOnError { AndroidUtils.logException(it) }
            .retry()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { groups ->
                    storedGroups.onNext(groups)
                    uiState.onNext(
                        uiState.value!!.copy(storedGroupsLoadedEvent = UiEvent(isTriggered = true))
                    )
                }
            )

        downloadFacultyFilter()
    }

    fun isGroupKeyDefault(): Boolean {
        return uiState.value?.groupFilterUiState?.key == String.DEFAULT
    }

    fun setFacultyKey(key: String) {
        uiState.onNext(
            uiState.value!!.copy(
                facultyFilterUiState = uiState.value!!.facultyFilterUiState.copy(key = key)
            )
        )

        downloadCourseFilter()
    }

    fun setCourseKey(key: String) {
        uiState.onNext(
            uiState.value!!.copy(
                courseFilterUiState = uiState.value!!.courseFilterUiState.copy(key = key)
            )
        )

        downloadGroupFilter()
    }

    fun setGroupKey(key: String) {
        uiState.onNext(
            uiState.value!!.copy(groupFilterUiState = uiState.value!!.groupFilterUiState.copy(key = key))
        )
    }

    fun resetCourses() {
        courseNetworkRequestDisposable.dispose()
        uiState.onNext(
            uiState.value!!.copy(courseFilterUiState = ScheduleFilterUiState())
        )
    }

    fun resetGroups() {
        groupsNetworkRequestDisposable.dispose()
        uiState.onNext(
            uiState.value!!.copy(groupFilterUiState = ScheduleFilterUiState())
        )
    }

    private fun downloadFacultyFilter() {
        assertDebug { uiState.value!!.facultyFilterUiState == ScheduleFilterUiState() }

        uiState.onNext(
            uiState.value!!.copy(facultyFilterUiState = uiState.value!!.facultyFilterUiState.copy(isLoading = true))
        )

        disposables += networkRepository.getFaculties()
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter ->
                    uiState.onNext(
                        uiState.value!!.copy(
                            facultyFilterUiState = uiState.value!!.facultyFilterUiState.copy(
                                isLoading = false,
                                dataState = DataState(data = scheduleFilter)
                            )
                        )
                    )
                },
                onError = { throwable ->
                    val error = Utils.getErrorType(throwable)
                    uiState.onNext(
                        uiState.value!!.copy(
                            facultyFilterUiState = uiState.value!!.facultyFilterUiState.copy(isLoading = false),
                            errorEvent = ErrorUiEvent(error = error)
                        )
                    )
                }
            )
    }

    private fun downloadCourseFilter() {
        assertDebug { uiState.value!!.courseFilterUiState == ScheduleFilterUiState() }

        uiState.onNext(
            uiState.value!!.copy(courseFilterUiState = ScheduleFilterUiState(isLoading = true))
        )

        val facultyKey = uiState.value!!.facultyFilterUiState.key
        courseNetworkRequestDisposable = networkRepository.getCourses(facultyKey.toInt())
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter ->
                    uiState.onNext(
                        uiState.value!!.copy(
                            courseFilterUiState = uiState.value!!.courseFilterUiState.copy(
                                isLoading = false,
                                dataState = DataState(data = scheduleFilter)
                            )
                        )
                    )
                },
                onError = { throwable ->
                    val error = Utils.getErrorType(throwable)
                    uiState.onNext(
                        uiState.value!!.copy(
                            courseFilterUiState = uiState.value!!.courseFilterUiState.copy(isLoading = false),
                            errorEvent = ErrorUiEvent(error = error)
                        )
                    )
                }
            )
    }

    private fun downloadGroupFilter() {
        assertDebug { uiState.value!!.groupFilterUiState == ScheduleFilterUiState() }

        uiState.onNext(
            uiState.value!!.copy(groupFilterUiState = ScheduleFilterUiState(isLoading = true))
        )

        val facultyKey = uiState.value!!.facultyFilterUiState.key
        val courseKey = uiState.value!!.courseFilterUiState.key
        groupsNetworkRequestDisposable =
            networkRepository.getGroups(facultyKey.toInt(), courseKey.toInt(), currentDate.academicYear)
                .doOnError { AndroidUtils.logException(it) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                    onSuccess = { scheduleFilter ->
                        uiState.onNext(
                            uiState.value!!.copy(
                                groupFilterUiState = uiState.value!!.groupFilterUiState.copy(
                                    isLoading = false,
                                    dataState = DataState(data = scheduleFilter)
                                )
                            )
                        )
                    },
                    onError = { throwable ->
                        val error = Utils.getErrorType(throwable)
                        uiState.onNext(
                            uiState.value!!.copy(
                                groupFilterUiState = uiState.value!!.groupFilterUiState.copy(isLoading = false),
                                errorEvent = ErrorUiEvent(error = error)
                            )
                        )
                    }
                )
    }

    fun isGroupAdded(key: String): Boolean {
        val facultyKey = uiState.value!!.facultyFilterUiState.key
        val facultyData = uiState.value!!.facultyFilterUiState.dataState.data
        assertDebug { facultyData != null }
        val groupData = uiState.value!!.groupFilterUiState.dataState.data
        assertDebug { groupData != null }
        return storedGroups.value
            ?.filter { group -> group.year == currentDate.academicYear }
            ?.filter { group -> group.faculty.toString(10) == facultyKey }
            ?.any { group ->
                val facultyName = facultyData!!.getValueOrDefault(facultyKey)
                val groupName = groupData!!.getValueOrDefault(key)
                group.value == formatGroupName(groupName, facultyName)
            } == true
    }

    fun createSelectedStudyGroupObject(): StudyGroup {
        val facultyKey = uiState.value!!.facultyFilterUiState.key
        val facultyData = uiState.value!!.facultyFilterUiState.dataState.data
        assertDebug { facultyData != null }
        val courseKey = uiState.value!!.courseFilterUiState.key
        val courseData = uiState.value!!.courseFilterUiState.dataState.data
        assertDebug { courseData != null }
        val groupKey = uiState.value!!.groupFilterUiState.key
        val groupData = uiState.value!!.groupFilterUiState.dataState.data
        assertDebug { groupData != null }
        val facultyName = facultyData!!.getValueOrDefault(facultyKey)
        val groupName = groupData!!.getValueOrDefault(groupKey)
        val name = formatGroupName(groupName, facultyName)
        return StudyGroup(AUTO_ID, groupKey, name, currentDate.academicYear, facultyKey.toInt(), courseKey.toInt())
    }

    fun storedGroupsLoadedEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(storedGroupsLoadedEvent = UiEvent())
        )
    }

    fun errorEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(errorEvent = ErrorUiEvent())
        )
    }

    private fun formatGroupName(groupName: String, facultyName: String): String {
        return if (sharedPreferencesRepository.getCurrentNetworkApiVersion() == NetworkApiVersion.MYUNIVERSITY) {
            "$groupName ($facultyName)"
        } else {
            groupName
        }
    }

    override fun onCleared() {
        disposables.clear()
        courseNetworkRequestDisposable.dispose()
        groupsNetworkRequestDisposable.dispose()
    }
}
