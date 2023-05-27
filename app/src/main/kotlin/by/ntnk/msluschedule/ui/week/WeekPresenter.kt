package by.ntnk.msluschedule.ui.week

import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.db.DatabaseDataMapper
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import org.threeten.bp.DayOfWeek
import javax.inject.Inject

data class ScheduleUiState(
    val isCheckingWeekStatus: Boolean = false,
    val isInitializingWeek: Boolean = false,
    val isWeekInitializedNoData: Boolean = false,
    val isProcessingFirstUpdate: Boolean = false,
    val isWeekInitialized: Boolean = false,
    val data: List<WeekdayWithLessons<Lesson>> = emptyList(),
    val throwable: Throwable? = null,
    val isFirstUpdateError: Boolean = false
) {
    fun isData(): Boolean = data.isNotEmpty()
    fun isError(): Boolean = throwable != null
}

data class ScheduleUpdateEvent(
    val isLoading: Boolean = false,
    private var isUpdateSuccessful: Boolean = false,
    private var throwable: Throwable? = null
) {
    val timestamp: Long = System.nanoTime()

    fun isEvent(): Boolean = isUpdateSuccessful

    fun consumeEvent() {
        isUpdateSuccessful = false
    }

    fun isError(): Boolean = throwable != null

    fun consumeError(): Throwable {
        val throwableCopy = throwable!!
        throwable = null
        return throwableCopy
    }
}

class WeekPresenter @Inject constructor(
    private val currentDate: CurrentDate,
    private val databaseRepository: DatabaseRepository,
    private val databaseDataMapper: DatabaseDataMapper,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val schedulerProvider: SchedulerProvider,
    private val networkRepository: NetworkRepository
) : Presenter<WeekView>() {

    private val scheduleUiState: BehaviorSubject<ScheduleUiState> = BehaviorSubject.createDefault(ScheduleUiState())
    val scheduleUiStateObservable: Observable<ScheduleUiState>
        get() = scheduleUiState

    private val scheduleUpdateEvent: BehaviorSubject<ScheduleUpdateEvent> =
        BehaviorSubject.createDefault(ScheduleUpdateEvent())
    val scheduleUpdateEventObservable: Observable<ScheduleUpdateEvent>
        get() = scheduleUpdateEvent

    fun getScheduleContainer(): ScheduleContainerInfo {
        return sharedPreferencesRepository.getSelectedScheduleContainer()
    }

    fun getSchedule(scheduleType: ScheduleType, weekId: Int) {
        getWeekdays(scheduleType, weekId)
            .toList()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { weekdays ->
                    val state = ScheduleUiState(data = weekdays)
                    scheduleUiState.onNext(state)
                },
                onError = { throwable ->
                    val state = ScheduleUiState(throwable = throwable)
                    scheduleUiState.onNext(state)
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
            .subscribeOn(schedulerProvider.single())
            .observeOn(schedulerProvider.ui())
    }

    private fun initializeWeek(weekId: Int) {
        scheduleUiState.onNext(ScheduleUiState(isInitializingWeek = true))
        databaseRepository.insertWeekdays(weekId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onComplete = { scheduleUiState.onNext(ScheduleUiState(isWeekInitializedNoData = true)) },
                onError = { throwable -> scheduleUiState.onNext(ScheduleUiState(throwable = throwable)) }
            )
    }

    fun updateSchedule(scheduleContainerId: Int, weekId: Int, isFirstUpdate: Boolean = false) {
        assert(scheduleUpdateEvent.hasValue())
        if (scheduleUpdateEvent.value!! != ScheduleUpdateEvent()) {
            return
        }

        if (isFirstUpdate) {
            scheduleUiState.onNext(ScheduleUiState(isProcessingFirstUpdate = true))
        } else {
            scheduleUpdateEvent.onNext(ScheduleUpdateEvent(isLoading = true))
        }
        databaseRepository.getScheduleContainer(scheduleContainerId)
            .flatMap { scheduleContainer ->
                when (scheduleContainer.type) {
                    ScheduleType.STUDYGROUP -> {
                        databaseRepository.getWeekKey(weekId)
                            .flatMapObservable {
                                val studyGroup = databaseDataMapper.mapToStudyGroup(scheduleContainer)
                                return@flatMapObservable networkRepository.getSchedule(studyGroup, it)
                            }
                            .toList()
                            .flatMap { weekdays ->
                                databaseRepository.deleteLessons(weekId, scheduleContainer.type)
                                    .andThen(Single.just(weekdays))
                            }
                            .flatMap { weekdays -> databaseRepository.insertStudyGroupSchedule(weekdays, weekId) }
                    }
                    ScheduleType.TEACHER -> {
                        databaseRepository.getWeekKey(weekId)
                            .flatMapObservable {
                                val teacher = databaseDataMapper.mapToTeacher(scheduleContainer)
                                return@flatMapObservable networkRepository.getSchedule(teacher, it)
                            }
                            .toList()
                            .flatMap { weekdays ->
                                databaseRepository.deleteLessons(weekId, scheduleContainer.type)
                                    .andThen(Single.just(weekdays))
                            }
                            .flatMap { weekdays -> databaseRepository.insertTeacherSchedule(weekdays, weekId) }
                    }
                    else -> Single.error(IllegalArgumentException())
                }
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { weekdays ->
                    val state = ScheduleUiState(data = weekdays)
                    scheduleUiState.onNext(state)

                    if (!isFirstUpdate) {
                        val event = ScheduleUpdateEvent(isUpdateSuccessful = true)
                        scheduleUpdateEvent.onNext(event)
                    }
                },
                onError = { throwable ->
                    if (isFirstUpdate) {
                        val state = ScheduleUiState(throwable = throwable, isFirstUpdateError = true)
                        scheduleUiState.onNext(state)
                    } else {
                        val updateState = ScheduleUpdateEvent(throwable = throwable)
                        scheduleUpdateEvent.onNext(updateState)
                    }
                }
            )
    }

    fun checkWeekInitialized(weekId: Int) {
        scheduleUiState.onNext(ScheduleUiState(isCheckingWeekStatus = true))
        databaseRepository.isWeekInitialized(weekId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { isInitialized ->
                    if (isInitialized) {
                        scheduleUiState.onNext(ScheduleUiState(isWeekInitialized = true))
                    } else {
                        initializeWeek(weekId)
                    }
                },
                onError = { throwable -> scheduleUiState.onNext(ScheduleUiState(throwable = throwable)) }
            )
    }

    fun getCurrentDayOfWeek(): Int {
        return if (currentDate.academicWeek >= 0) {
            currentDate.date.dayOfWeek.value
        } else {
            DayOfWeek.MONDAY.value
        }
    }
}
