package by.ntnk.msluschedule.ui.weekscontainer

import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.data.Week
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.ErrorType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import by.ntnk.msluschedule.utils.Utils
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

data class DataState(
    val data: List<Week>? = null,
    val currentAcademicWeekIndex: Int = Int.DEFAULT
)

data class UiEvent(
    val isTriggered: Boolean = false
)

data class ScheduleContainerDeletedUiEvent(
    val isTriggered: Boolean = false,
    val data: ScheduleContainer = ScheduleContainer.DEFAULT
)

data class ErrorUiEvent(
    val error: ErrorType? = null
)

data class ScheduleUiState(
    val dataState: DataState = DataState(),
    val academicWeekChangedEvent: UiEvent = UiEvent(),
    val scheduleContainerDeletedEvent: ScheduleContainerDeletedUiEvent = ScheduleContainerDeletedUiEvent(),
    val errorEvent: ErrorUiEvent = ErrorUiEvent()
)

class WeeksContainerPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val currentDate: CurrentDate,
    private val schedulerProvider: SchedulerProvider
) : Presenter<WeeksContainerView>() {

    private val uiState: BehaviorSubject<ScheduleUiState> = BehaviorSubject.createDefault(ScheduleUiState())
    val uiStateObservable: Observable<ScheduleUiState>
        get() = uiState

    private val disposables: CompositeDisposable = CompositeDisposable()

    private var academicWeek: Int = Int.DEFAULT

    fun getSelectedScheduleContainer(): ScheduleContainer {
        return sharedPreferencesRepository.getSelectedScheduleContainer()
    }

    fun updateCurrentAcademicWeek() {
        if (academicWeek != currentDate.academicWeek) {
            uiState.onNext(
                uiState.value!!.copy(academicWeekChangedEvent = UiEvent(isTriggered = true))
            )
        }
        academicWeek = currentDate.academicWeek
    }

    fun getData(scheduleContainer: ScheduleContainer) {
        disposables += databaseRepository.getWeeks(scheduleContainer.id)
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
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { (weeks, currentAcademicWeekIndex) ->
                    uiState.onNext(
                        uiState.value!!.copy(dataState = DataState(weeks, currentAcademicWeekIndex))
                    )
                },
                onError = { throwable ->
                    val error = Utils.getErrorType(throwable)
                    uiState.onNext(
                        uiState.value!!.copy(errorEvent = ErrorUiEvent(error = error))
                    )
                }
            )
    }

    fun deleteSelectedScheduleContainer() {
        val scheduleContainer = sharedPreferencesRepository.getSelectedScheduleContainer()

        disposables += databaseRepository.deleteScheduleContainer(scheduleContainer.id)
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onComplete = {
                    sharedPreferencesRepository.clearSelectedScheduleContainer()
                    uiState.onNext(
                        uiState.value!!.copy(
                            scheduleContainerDeletedEvent = ScheduleContainerDeletedUiEvent(
                                isTriggered = true,
                                data = scheduleContainer
                            )
                        )
                    )
                },
                onError = { throwable ->
                    val error = Utils.getErrorType(throwable)
                    uiState.onNext(
                        uiState.value!!.copy(errorEvent = ErrorUiEvent(error = error))
                    )
                }
            )
    }

    fun academicWeekChangedEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(academicWeekChangedEvent = UiEvent())
        )
    }

    fun scheduleContainerDeletedEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(scheduleContainerDeletedEvent = ScheduleContainerDeletedUiEvent())
        )
    }

    fun errorEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(errorEvent = ErrorUiEvent())
        )
    }

    override fun onCleared() {
        disposables.clear()
    }

    companion object {
        private const val WEEKS_NUM = 5
        private const val SIDE_WEEKS_NUM = WEEKS_NUM / 2
        const val CENTRAL_VIEW_POSITION = WEEKS_NUM / 2
    }
}
