package by.ntnk.msluschedule.ui.addteacher

import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.AUTO_ID
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.ErrorType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.Utils
import by.ntnk.msluschedule.utils.assertDebug
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
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

data class AddTeacherUiState(
    val teacherFilterUiState: ScheduleFilterUiState = ScheduleFilterUiState(),
    val storedTeachersLoadedEvent: UiEvent = UiEvent(),
    val errorEvent: ErrorUiEvent = ErrorUiEvent()
)

class AddTeacherPresenter @Inject constructor(
    databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider
) : Presenter<AddTeacherView>() {

    private val storedTeachers: BehaviorSubject<List<Teacher>> = BehaviorSubject.createDefault(emptyList())

    private val uiState: BehaviorSubject<AddTeacherUiState> = BehaviorSubject.createDefault(AddTeacherUiState())
    val uiStateObservable: Observable<AddTeacherUiState>
        get() = uiState

    init {
        databaseRepository.getTeachers()
            .toList()
            .retry()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { groups ->
                    storedTeachers.onNext(groups)
                    uiState.onNext(
                        uiState.value!!.copy(storedTeachersLoadedEvent = UiEvent(isTriggered = true))
                    )
                }
            )

        downloadTeacherScheduleFilter()
    }

    private fun downloadTeacherScheduleFilter() {
        uiState.onNext(
            uiState.value!!.copy(teacherFilterUiState = ScheduleFilterUiState(isLoading = true))
        )

        networkRepository.getTeachers()
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleFilter ->
                    uiState.onNext(
                        uiState.value!!.copy(
                            teacherFilterUiState = uiState.value!!.teacherFilterUiState.copy(
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
                            teacherFilterUiState = uiState.value!!.teacherFilterUiState.copy(isLoading = false),
                            errorEvent = ErrorUiEvent(error = error)
                        )
                    )
                }
            )
    }

    fun setTeacherKey(key: String) {
        uiState.onNext(
            uiState.value!!.copy(teacherFilterUiState = uiState.value!!.teacherFilterUiState.copy(key = key))
        )
    }

    fun isTeacherAdded(key: String): Boolean {
        val teacherData = uiState.value!!.teacherFilterUiState.dataState.data
        assertDebug { teacherData != null }
        return storedTeachers.value
            ?.filter { teacher -> teacher.year == currentDate.academicYear }
            ?.map { teacher -> teacher.value }
            ?.any { teacherName -> teacherName == teacherData!!.getValueOrDefault(key) } == true
    }

    fun createSelectedTeacherObject(): Teacher {
        val teacherKey = uiState.value!!.teacherFilterUiState.key
        val teacherData = uiState.value!!.teacherFilterUiState.dataState.data
        assertDebug { teacherData != null }
        val teacherName = teacherData!!.getValueOrDefault(teacherKey)
        return Teacher(AUTO_ID, teacherKey, teacherName, currentDate.academicYear)
    }

    fun storedTeachersLoadedEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(storedTeachersLoadedEvent = UiEvent())
        )
    }

    fun errorEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(errorEvent = ErrorUiEvent())
        )
    }
}
