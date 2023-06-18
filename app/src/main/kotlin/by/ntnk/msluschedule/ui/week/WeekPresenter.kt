package by.ntnk.msluschedule.ui.week

import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.data.WeekdayWithLessons
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

data class WeekUiState(
    val isCheckingWeekStatus: Boolean = false,
    val isInitializingWeek: Boolean = false,
    val isProcessingInitialUpdate: Boolean = false,
    val isWeekInitialized: Boolean = false,
    val data: List<WeekdayWithLessons<Lesson>>? = null,
    val throwable: Throwable? = null,
    val isInitialUpdateError: Boolean = false
) {
    fun isData(): Boolean = data != null
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
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val schedulerProvider: SchedulerProvider,
    private val networkRepository: NetworkRepository
) : Presenter<WeekView>() {

    private val weekUiState: BehaviorSubject<WeekUiState> = BehaviorSubject.createDefault(WeekUiState())
    val weekUiStateObservable: Observable<WeekUiState>
        get() = weekUiState

    private val scheduleUpdateEvent: BehaviorSubject<ScheduleUpdateEvent> =
        BehaviorSubject.createDefault(ScheduleUpdateEvent())
    val scheduleUpdateEventObservable: Observable<ScheduleUpdateEvent>
        get() = scheduleUpdateEvent

    fun getScheduleContainer(): ScheduleContainer {
        return sharedPreferencesRepository.getSelectedScheduleContainer()
    }

    fun getSchedule(scheduleType: ScheduleType, weekId: Int) {
        getWeekdays(scheduleType, weekId)
            .toList()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { weekdays ->
                    val state = WeekUiState(data = weekdays)
                    weekUiState.onNext(state)
                },
                onError = { throwable ->
                    val state = WeekUiState(throwable = throwable)
                    weekUiState.onNext(state)
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
        weekUiState.onNext(WeekUiState(isInitializingWeek = true))
        databaseRepository.insertWeekdays(weekId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onComplete = {
                    val scheduleContainer = getScheduleContainer()
                    updateSchedule(scheduleContainer, weekId, isInitialUpdate = true)
                },
                onError = { throwable -> weekUiState.onNext(WeekUiState(throwable = throwable)) }
            )
    }

    fun updateSchedule(scheduleContainer: ScheduleContainer, weekId: Int) {
        updateSchedule(scheduleContainer, weekId, isInitialUpdate = false)
    }

    private fun updateSchedule(scheduleContainer: ScheduleContainer, weekId: Int, isInitialUpdate: Boolean) {
        assert(scheduleUpdateEvent.hasValue())
        if (scheduleUpdateEvent.value!! != ScheduleUpdateEvent()) {
            return
        }

        if (isInitialUpdate) {
            weekUiState.onNext(WeekUiState(isProcessingInitialUpdate = true))
        } else {
            scheduleUpdateEvent.onNext(ScheduleUpdateEvent(isLoading = true))
        }

        when (scheduleContainer.type) {
            ScheduleType.STUDYGROUP -> updateScheduleGroup(scheduleContainer, weekId)
            ScheduleType.TEACHER -> updateScheduleTeacher(scheduleContainer, weekId)
            else -> Single.error(IllegalArgumentException())
        }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { weekdays ->
                    val state = WeekUiState(data = weekdays)
                    weekUiState.onNext(state)

                    if (!isInitialUpdate) {
                        val event = ScheduleUpdateEvent(isUpdateSuccessful = true)
                        scheduleUpdateEvent.onNext(event)
                    }
                },
                onError = { throwable ->
                    if (isInitialUpdate) {
                        val state = WeekUiState(throwable = throwable, isInitialUpdateError = true)
                        weekUiState.onNext(state)
                    } else {
                        val updateState = ScheduleUpdateEvent(throwable = throwable)
                        scheduleUpdateEvent.onNext(updateState)
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

    fun checkWeekInitialized(weekId: Int) {
        weekUiState.onNext(WeekUiState(isCheckingWeekStatus = true))
        databaseRepository.isWeekInitialized(weekId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { isInitialized ->
                    if (isInitialized) {
                        weekUiState.onNext(WeekUiState(isWeekInitialized = true))
                    } else {
                        initializeWeek(weekId)
                    }
                },
                onError = { throwable -> weekUiState.onNext(WeekUiState(throwable = throwable)) }
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
