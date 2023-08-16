package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.ErrorType
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import javax.inject.Inject

object NewScheduleContainerError : ErrorType

data class ErrorUiState(
    val errorType: ErrorType? = null
)

data class MainUiState(
    val selectedScheduleContainer: ScheduleContainer = ScheduleContainer.DEFAULT,
    val scheduleContainers: List<ScheduleContainer> = emptyList(),
    val isLoading: Boolean = false,
    val errorUiState: ErrorUiState = ErrorUiState()
)

class MainPresenter @Inject constructor(
    private val currentDate: CurrentDate,
    private val databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val schedulerProvider: SchedulerProvider
) : Presenter<MainView>() {

    private val mainUiState: BehaviorSubject<MainUiState> = BehaviorSubject.createDefault(MainUiState())
    val mainUiStateObservable: Observable<MainUiState>
        get() = mainUiState

    private val disposables: CompositeDisposable = CompositeDisposable()

    init {
        disposables += databaseRepository.getScheduleContainersReactive()
            .map { scheduleContainers ->
                scheduleContainers.map { scheduleContainer -> formatScheduleContainerValue(scheduleContainer) }
            }
            .retry()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onNext = { scheduleContainers ->
                    mainUiState.onNext(mainUiState.value!!.copy(scheduleContainers = scheduleContainers))
                }
            )

        val scheduleContainer = sharedPreferencesRepository.getSelectedScheduleContainer()
        val uiScheduleContainer = formatScheduleContainerValue(scheduleContainer)
        mainUiState.onNext(mainUiState.value!!.copy(selectedScheduleContainer = uiScheduleContainer))
    }

    fun addGroup(studyGroup: StudyGroup) {
        mainUiState.onNext(mainUiState.value!!.copy(isLoading = true))

        disposables += databaseRepository.insertStudyGroup(studyGroup)
            .flatMap { id -> insertWeeksWithWeekdays(id).andThen(Single.just(id)) }
            .doOnError { throwable -> Timber.e(throwable) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { id ->
                    val scheduleContainer =
                        ScheduleContainer(id, studyGroup.value, ScheduleType.STUDYGROUP, studyGroup.year)
                    sharedPreferencesRepository.setSelectedScheduleContainer(scheduleContainer)

                    val uiScheduleContainer = formatScheduleContainerValue(scheduleContainer)
                    mainUiState.onNext(
                        mainUiState.value!!.copy(
                            selectedScheduleContainer = uiScheduleContainer,
                            isLoading = false
                        )
                    )
                },
                onError = {
                    mainUiState.onNext(
                        mainUiState.value!!.copy(
                            errorUiState = ErrorUiState(errorType = NewScheduleContainerError),
                            isLoading = false
                        )
                    )
                }
            )
    }

    fun addTeacher(teacher: Teacher) {
        mainUiState.onNext(mainUiState.value!!.copy(isLoading = true))

        disposables += databaseRepository.insertTeacher(teacher)
            .flatMap { id -> insertWeeksWithWeekdays(id).andThen(Single.just(id)) }
            .doOnError { throwable -> Timber.e(throwable) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { id ->
                    val scheduleContainer = ScheduleContainer(id, teacher.value, ScheduleType.TEACHER, teacher.year)
                    sharedPreferencesRepository.setSelectedScheduleContainer(scheduleContainer)

                    val uiScheduleContainer = formatScheduleContainerValue(scheduleContainer)
                    mainUiState.onNext(
                        mainUiState.value!!.copy(
                            selectedScheduleContainer = uiScheduleContainer,
                            isLoading = false
                        )
                    )
                },
                onError = {
                    mainUiState.onNext(
                        mainUiState.value!!.copy(
                            errorUiState = ErrorUiState(errorType = NewScheduleContainerError),
                            isLoading = false
                        )
                    )
                }
            )
    }

    private fun insertWeeksWithWeekdays(scheduleContainerId: Int): Completable {
        return networkRepository.getWeeks()
            .flatMapObservable { databaseRepository.insertWeeksGetIds(it, scheduleContainerId) }
            .ignoreElements()
            .doOnError {
                databaseRepository.deleteScheduleContainer(scheduleContainerId)
                    .doOnError { throwable -> Timber.e(throwable) }
                    .subscribe()
            }
    }

    fun clearSelectedScheduleContainer() {
        sharedPreferencesRepository.clearSelectedScheduleContainer()

        val scheduleContainer = sharedPreferencesRepository.getSelectedScheduleContainer()
        val uiScheduleContainer = formatScheduleContainerValue(scheduleContainer)
        mainUiState.onNext(mainUiState.value!!.copy(selectedScheduleContainer = uiScheduleContainer))
    }

    fun changeScheduleContainer(scheduleContainerId: Int) {
        mainUiState.onNext(mainUiState.value!!.copy(isLoading = true))

        disposables += databaseRepository.getScheduleContainer(scheduleContainerId)
            .retry()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleContainer ->
                    sharedPreferencesRepository.setSelectedScheduleContainer(scheduleContainer)

                    val uiScheduleContainer = formatScheduleContainerValue(scheduleContainer)
                    mainUiState.onNext(
                        mainUiState.value!!.copy(
                            selectedScheduleContainer = uiScheduleContainer,
                            isLoading = false
                        )
                    )
                }
            )
    }

    fun errorShown() {
        mainUiState.onNext(mainUiState.value!!.copy(errorUiState = ErrorUiState(errorType = null)))
    }

    private fun formatScheduleContainerValue(scheduleContainer: ScheduleContainer): ScheduleContainer {
        val value = if (scheduleContainer.year != currentDate.academicYear) {
            "(${scheduleContainer.year}) ${scheduleContainer.value}"
        } else {
            scheduleContainer.value
        }
        return scheduleContainer.copy(value = value)
    }

    fun getSelectedScheduleContainer(): ScheduleContainer {
        return mainUiState.value!!.selectedScheduleContainer
    }

    fun isFabShown(): Boolean {
        return sharedPreferencesRepository.isMainFabShown()
    }

    fun isOnboardingNeeded(): Boolean {
        return sharedPreferencesRepository.isFirstAppLaunch
    }

    fun completeOnboarding() {
        sharedPreferencesRepository.isFirstAppLaunch = false
    }

    fun clear() {
        disposables.clear()
    }
}
