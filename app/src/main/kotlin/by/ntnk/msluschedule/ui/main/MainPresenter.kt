package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

class MainPresenter @Inject constructor(
    private val currentDate: CurrentDate,
    private val databaseRepository: DatabaseRepository,
    private val networkRepository: NetworkRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val schedulerProvider: SchedulerProvider
) : Presenter<MainView>() {

    private val disposables: CompositeDisposable = CompositeDisposable()

    fun addGroup(studyGroup: StudyGroup) {
        databaseRepository.insertStudyGroup(studyGroup)
            .flatMap { insertWeeksWithWeekdays(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { id ->
                    val scheduleContainer =
                        ScheduleContainer(id, studyGroup.value, ScheduleType.STUDYGROUP, studyGroup.year)
                    sharedPreferencesRepository.putSelectedScheduleContainer(scheduleContainer)
                    view?.changeScheduleContainer(scheduleContainer)
                    view?.addScheduleContainerMenuItem(scheduleContainer)
                    view?.selectScheduleContainerMenuItem(scheduleContainer)
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    view?.showError()
                }
            )
    }

    fun addTeacher(teacher: Teacher) {
        databaseRepository.insertTeacher(teacher)
            .flatMap { insertWeeksWithWeekdays(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { teacherId ->
                    val scheduleContainer =
                        ScheduleContainer(teacherId, teacher.value, ScheduleType.TEACHER, teacher.year)
                    sharedPreferencesRepository.putSelectedScheduleContainer(scheduleContainer)
                    view?.changeScheduleContainer(scheduleContainer)
                    view?.addScheduleContainerMenuItem(scheduleContainer)
                    view?.selectScheduleContainerMenuItem(scheduleContainer)
                },
                onError = { throwable ->
                    Timber.e(throwable)
                    view?.showError()
                }
            )
    }

    private fun insertWeeksWithWeekdays(containerId: Int): Single<Int> {
        return networkRepository.getWeeks()
            .flatMapObservable { databaseRepository.insertWeeksGetIds(it, containerId) }
            .ignoreElements()
            .doOnError {
                databaseRepository.deleteScheduleContainer(containerId)
                    .subscribeBy(onError = { throwable -> Timber.e(throwable) })
            }
            .andThen(Single.just(containerId))
    }

    fun initContainerListView() {
        disposables += databaseRepository.getScheduleContainers()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onNext = { scheduleContainer ->
                    val value = if (scheduleContainer.year != currentDate.academicYear) {
                        "(${scheduleContainer.year}) ${scheduleContainer.value}"
                    } else {
                        scheduleContainer.value
                    }
                    view?.addScheduleContainerMenuItem(scheduleContainer.copy(value = value))
                },
                onError = { throwable -> Timber.e(throwable) },
                onComplete = {
                    val scheduleContainer = sharedPreferencesRepository.getSelectedScheduleContainer()
                    view?.selectScheduleContainerMenuItem(scheduleContainer)
                }
            )
    }

    fun changeScheduleContainer(scheduleContainerId: Int) {
        disposables += databaseRepository.getScheduleContainer(scheduleContainerId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { scheduleContainer ->
                    sharedPreferencesRepository.putSelectedScheduleContainer(scheduleContainer)

                    val value = if (scheduleContainer.year != currentDate.academicYear) {
                        "(${scheduleContainer.year}) ${scheduleContainer.value}"
                    } else {
                        scheduleContainer.value
                    }
                    view?.changeScheduleContainer(scheduleContainer.copy(value = value))
                },
                onError = { throwable -> Timber.e(throwable) }
            )
    }

    fun isSelectedContainerDefault(): Boolean {
        return sharedPreferencesRepository.getSelectedScheduleContainer().type == ScheduleType.DEFAULT
    }

    fun clearDisposables() = disposables.clear()
}
