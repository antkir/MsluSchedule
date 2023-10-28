package by.ntnk.msluschedule.ui.week

import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.ErrorType
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import by.ntnk.msluschedule.utils.Utils
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import org.threeten.bp.DayOfWeek
import javax.inject.Inject

data class DataState(
    val data: List<WeekdayWithLessons<Lesson>>? = null
)

data class UiEvent(
    val isTriggered: Boolean = false
)

data class ErrorUiEvent(
    val isScheduleUpdateError: Boolean = false,
    val error: ErrorType? = null
)

data class WeekUiState(
    val isUiStateInitialized: Boolean = false,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val dataState: DataState = DataState(),
    val successfulScheduleUpdateEvent: UiEvent = UiEvent(),
    val errorEvent: ErrorUiEvent = ErrorUiEvent()
)

class WeekPresenter @Inject constructor(
    private val currentDate: CurrentDate,
    private val databaseRepository: DatabaseRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val schedulerProvider: SchedulerProvider,
    private val networkRepository: NetworkRepository
) : Presenter<WeekView>() {

    private val uiState: BehaviorSubject<WeekUiState> = BehaviorSubject.createDefault(WeekUiState())
    val uiStateObservable: Observable<WeekUiState>
        get() = uiState

    fun getSelectedScheduleContainer(): ScheduleContainer {
        return sharedPreferencesRepository.getSelectedScheduleContainer()
    }

    private fun getLocalData(scheduleType: ScheduleType, weekId: Int) {
        getWeekdays(scheduleType, weekId)
            .toList()
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { weekdays ->
                    uiState.onNext(
                        uiState.value!!.copy(
                            isLoading = false,
                            dataState = DataState(data = weekdays)
                        )
                    )
                },
                onError = { throwable ->
                    val error = Utils.getErrorType(throwable)
                    uiState.onNext(
                        uiState.value!!.copy(
                            isLoading = false,
                            errorEvent = ErrorUiEvent(error = error)
                        )
                    )
                }
            )
    }

    private fun getWeekdays(type: ScheduleType, weekId: Int): Observable<WeekdayWithLessons<Lesson>> {
        return when (type) {
            ScheduleType.STUDYGROUP -> databaseRepository.getWeekdaysWithStudyGroupLessons(weekId)
            ScheduleType.TEACHER -> databaseRepository.getWeekdaysWithTeacherLessons(weekId)
            else -> Observable.error(IllegalArgumentException())
        }
    }

    fun hasNotes(weekdayId: Int): Single<Boolean> {
        return databaseRepository.checkWeekdayHasNotes(weekdayId)
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.single())
            .observeOn(schedulerProvider.ui())
    }

    private fun initializeWeek(scheduleContainer: ScheduleContainer, weekId: Int) {
        uiState.onNext(
            uiState.value!!.copy(isLoading = true)
        )

        databaseRepository.insertWeekdays(weekId)
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onComplete = { updateSchedule(scheduleContainer, weekId, isInitializationUpdate = true) },
                onError = { throwable ->
                    val error = Utils.getErrorType(throwable)
                    uiState.onNext(
                        uiState.value!!.copy(
                            isLoading = false,
                            errorEvent = ErrorUiEvent(error = error)
                        )
                    )
                }
            )
    }

    fun updateSchedule(scheduleContainer: ScheduleContainer, weekId: Int) {
        if (uiState.value?.isLoading == true || uiState.value?.isUpdating == true) {
            return
        }

        uiState.onNext(
            uiState.value!!.copy(isUpdating = true)
        )

        updateSchedule(scheduleContainer, weekId, isInitializationUpdate = false)
    }

    private fun updateSchedule(scheduleContainer: ScheduleContainer, weekId: Int, isInitializationUpdate: Boolean) {
        when (scheduleContainer.type) {
            ScheduleType.STUDYGROUP -> updateScheduleGroup(scheduleContainer, weekId)
            ScheduleType.TEACHER -> updateScheduleTeacher(scheduleContainer, weekId)
            else -> Single.error(IllegalArgumentException())
        }
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { weekdays ->
                    uiState.onNext(
                        uiState.value!!.copy(
                            isLoading = false,
                            isUpdating = false,
                            dataState = DataState(data = weekdays),
                            successfulScheduleUpdateEvent = UiEvent(isTriggered = !isInitializationUpdate)
                        )
                    )
                },
                onError = { throwable ->
                    val error = Utils.getErrorType(throwable)
                    if (isInitializationUpdate) {
                        uiState.onNext(
                            uiState.value!!.copy(errorEvent = ErrorUiEvent(error = error))
                        )

                        getLocalData(scheduleContainer.type, weekId)
                    } else {
                        uiState.onNext(
                            uiState.value!!.copy(
                                isUpdating = false,
                                errorEvent = ErrorUiEvent(
                                    isScheduleUpdateError = true,
                                    error = error
                                )
                            )
                        )
                    }
                }
            )
    }

    private fun updateScheduleGroup(
        scheduleContainer: ScheduleContainer,
        weekId: Int
    ): Single<List<WeekdayWithLessons<Lesson>>> {
        return databaseRepository.getStudyGroup(scheduleContainer.id)
            .zipWith(databaseRepository.getWeekKey(weekId)) { studyGroup, weekKey -> Pair(studyGroup, weekKey) }
            .flatMapObservable { (studyGroup, weekKey) -> networkRepository.getSchedule(studyGroup, weekKey) }
            .toList()
            .flatMap { weekdays ->
                databaseRepository.deleteLessons(weekId, scheduleContainer.type)
                    .andThen(Single.just(weekdays))
            }
            .flatMap { weekdays -> databaseRepository.insertStudyGroupSchedule(weekdays, weekId) }
    }

    private fun updateScheduleTeacher(
        scheduleContainer: ScheduleContainer,
        weekId: Int
    ): Single<List<WeekdayWithLessons<Lesson>>> {
        return databaseRepository.getTeacher(scheduleContainer.id)
            .zipWith(databaseRepository.getWeekKey(weekId)) { teacher, weekKey -> Pair(teacher, weekKey) }
            .flatMapObservable { (teacher, weekKey) -> networkRepository.getSchedule(teacher, weekKey) }
            .toList()
            .flatMap { weekdays ->
                databaseRepository.deleteLessons(weekId, scheduleContainer.type)
                    .andThen(Single.just(weekdays))
            }
            .flatMap { weekdays -> databaseRepository.insertTeacherSchedule(weekdays, weekId) }
    }

    fun getData(scheduleContainer: ScheduleContainer, weekId: Int) {
        uiState.onNext(
            uiState.value!!.copy(isUiStateInitialized = true)
        )

        databaseRepository.isWeekInitialized(weekId)
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { isInitialized ->
                    if (isInitialized) {
                        getLocalData(scheduleContainer.type, weekId)
                    } else {
                        initializeWeek(scheduleContainer, weekId)
                    }
                },
                onError = { throwable ->
                    val error = Utils.getErrorType(throwable)
                    uiState.onNext(
                        uiState.value!!.copy(errorEvent = ErrorUiEvent(error = error))
                    )
                }
            )
    }

    fun getCurrentDayOfWeek(): Int {
        return if (currentDate.academicWeek >= 0) {
            currentDate.date.dayOfWeek.value
        } else {
            DayOfWeek.MONDAY.value
        }
    }

    fun successfulScheduleUpdateEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(successfulScheduleUpdateEvent = UiEvent())
        )
    }

    fun errorEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(errorEvent = ErrorUiEvent())
        )
    }
}
