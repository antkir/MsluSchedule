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
            .observeOn(schedulerProvider.ui())
            .doOnSuccess { studyGroupId ->
                val scheduleContainer = ScheduleContainer(studyGroupId, studyGroup.name, ScheduleType.STUDYGROUP)
                view?.showNewScheduleContainerLoading(scheduleContainer)
            }
            .observeOn(schedulerProvider.io())
            .flatMap { insertWeeksWithWeekdays(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .doOnError { view?.showError() }
            .subscribeBy(
                onSuccess = { studyGroupId ->
                    val scheduleContainer =
                        ScheduleContainer(studyGroupId, studyGroup.name, ScheduleType.STUDYGROUP)
                    sharedPreferencesRepository.putSelectedScheduleContainer(scheduleContainer)
                    view?.initMainContent()
                    view?.addScheduleContainerMenuItem(scheduleContainer)
                    view?.checkScheduleContainerMenuItem(scheduleContainer)
                },
                onError = { throwable -> Timber.e(throwable) }
            )
    }

    fun addTeacher(teacher: Teacher) {
        databaseRepository.insertTeacher(teacher)
            .observeOn(schedulerProvider.ui())
            .doOnSuccess { teacherId ->
                val scheduleContainer = ScheduleContainer(teacherId, teacher.name, ScheduleType.TEACHER)
                view?.showNewScheduleContainerLoading(scheduleContainer)
            }
            .observeOn(schedulerProvider.io())
            .flatMap { insertWeeksWithWeekdays(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .doOnError { view?.showError() }
            .subscribeBy(
                onSuccess = { teacherId ->
                    val scheduleContainer = ScheduleContainer(teacherId, teacher.name, ScheduleType.TEACHER)
                    sharedPreferencesRepository.putSelectedScheduleContainer(scheduleContainer)
                    view?.initMainContent()
                    view?.addScheduleContainerMenuItem(scheduleContainer)
                    view?.checkScheduleContainerMenuItem(scheduleContainer)
                },
                onError = { throwable -> Timber.e(throwable) }
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
                onNext = { dbScheduleContainer ->
                    val name = if (dbScheduleContainer.year != currentDate.academicYear) {
                        "(${dbScheduleContainer.year}) ${dbScheduleContainer.name}"
                    } else {
                        dbScheduleContainer.name
                    }
                    val scheduleContainer = ScheduleContainer(dbScheduleContainer.id, name, dbScheduleContainer.type)
                    view?.addScheduleContainerMenuItem(scheduleContainer)
                },
                onError = { throwable -> Timber.e(throwable) },
                onComplete = {
                    val scheduleContainer = sharedPreferencesRepository.getSelectedScheduleContainer()
                    view?.checkScheduleContainerMenuItem(scheduleContainer)
                }
            )
    }

    fun isSelectedContainerDefault(): Boolean {
        return sharedPreferencesRepository.getSelectedScheduleContainer().type == ScheduleType.DEFAULT
    }

    fun clearDisposables() = disposables.clear()
}
