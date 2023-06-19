package by.ntnk.msluschedule.ui.weekscontainer

import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.data.Week
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

data class ScheduleUiState(
    val isLoading: Boolean = false,
    val weeks: List<Week>? = null,
    val currentAcademicWeekIndex: Int = Int.DEFAULT,
    val throwable: Throwable? = null
) {
    fun isData(): Boolean = weeks != null && currentAcademicWeekIndex != Int.DEFAULT
    fun isError(): Boolean = throwable != null
}

data class ScheduleContainerDeletedEvent(
    private var data: ScheduleContainer? = null,
    private var throwable: Throwable? = null
) {
    fun isEvent(): Boolean = data != null

    fun consumeEvent(): ScheduleContainer {
        val dataCopy = data!!
        data = null
        return dataCopy
    }

    fun isError(): Boolean = throwable != null

    fun consumeError(): Throwable {
        val throwableCopy = throwable!!
        throwable = null
        return throwableCopy
    }
}

class WeeksContainerPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider
) : Presenter<WeeksContainerView>() {

    private val academicWeek: BehaviorSubject<Int> = BehaviorSubject.createDefault(Int.DEFAULT)

    private val scheduleUiState: BehaviorSubject<ScheduleUiState> = BehaviorSubject.createDefault(ScheduleUiState())
    val scheduleUiStateObservable: Observable<ScheduleUiState>
        get() = scheduleUiState

    private val scheduleContainerDeletedEvent: BehaviorSubject<ScheduleContainerDeletedEvent> =
        BehaviorSubject.createDefault(ScheduleContainerDeletedEvent())
    val scheduleContainerDeletedEventObservable: Observable<ScheduleContainerDeletedEvent>
        get() = scheduleContainerDeletedEvent

    fun getSelectedScheduleContainer(): ScheduleContainer {
        return sharedPreferencesRepository.getSelectedScheduleContainer()
    }

    fun isAcademicWeekChanged(): Boolean {
        val academicWeekPrev = academicWeek.value
        academicWeek.onNext(currentDate.academicWeek)
        return academicWeek.value != academicWeekPrev
    }

    fun getWeeks(scheduleContainerId: Int) {
        scheduleUiState.onNext(ScheduleUiState(isLoading = true))
        databaseRepository.getWeeks(scheduleContainerId)
            .toList()
            .map { weeks ->
                val currentAcademicWeekIndex = minOf(weeks.lastIndex, maxOf(currentDate.academicWeek, 0))
                val filteredWeeks = mutableListOf<Week>()
                for (i in currentAcademicWeekIndex - SIDE_WEEKS_NUM..currentAcademicWeekIndex + SIDE_WEEKS_NUM) {
                    if (i in weeks.indices) {
                        filteredWeeks.add(weeks[i])
                    }
                }
                Pair(filteredWeeks, currentAcademicWeekIndex)
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { (weeks, currentAcademicWeekIndex) ->
                    val data = ScheduleUiState(weeks = weeks, currentAcademicWeekIndex = currentAcademicWeekIndex)
                    scheduleUiState.onNext(data)
                },
                onError = { throwable -> scheduleUiState.onNext(ScheduleUiState(throwable = throwable)) }
            )
    }

    fun deleteScheduleContainer(scheduleContainer: ScheduleContainer) {
        databaseRepository.deleteScheduleContainer(scheduleContainer.id)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onComplete = {
                    sharedPreferencesRepository.selectEmptyScheduleContainer()
                    scheduleContainerDeletedEvent.onNext(ScheduleContainerDeletedEvent(data = scheduleContainer))
                },
                onError = { throwable ->
                    scheduleContainerDeletedEvent.onNext(ScheduleContainerDeletedEvent(throwable = throwable))
                }
            )
    }

    companion object {
        private const val WEEKS_NUM = 5
        private const val SIDE_WEEKS_NUM = WEEKS_NUM / 2
        const val CENTRAL_VIEW_POSITION = WEEKS_NUM / 2
    }
}
