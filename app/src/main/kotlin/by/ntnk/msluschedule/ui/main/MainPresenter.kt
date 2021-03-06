package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.data.ScheduleContainerInfo
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
        private val networkRepository: dagger.Lazy<NetworkRepository>,
        private val sharedPreferencesRepository: SharedPreferencesRepository,
        private val schedulerProvider: SchedulerProvider
) : Presenter<MainView>() {
    private val disposables: CompositeDisposable = CompositeDisposable()

    fun addGroup(studyGroup: StudyGroup) {
        databaseRepository.insertStudyGroup(studyGroup)
                .observeOn(schedulerProvider.ui())
                .doOnSuccess { studyGroupId ->
                    val info = ScheduleContainerInfo(studyGroupId, studyGroup.name, ScheduleType.STUDYGROUP)
                    view?.showNewScheduleContainerLoading(info)
                }
                .observeOn(schedulerProvider.single())
                .flatMap { insertWeeksWithWeekdays(it) }
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .doOnError { view?.showError() }
                .subscribeBy(
                        onSuccess = { studyGroupId ->
                            val info = ScheduleContainerInfo(studyGroupId, studyGroup.name, ScheduleType.STUDYGROUP)
                            sharedPreferencesRepository.putSelectedScheduleContainer(info)
                            view?.initMainContent()
                            view?.addScheduleContainerMenuItem(info)
                            view?.checkScheduleContainerMenuItem(info)
                        },
                        onError = { throwable -> Timber.e(throwable) }
                )
    }

    fun addTeacher(teacher: Teacher) {
        databaseRepository.insertTeacher(teacher)
                .observeOn(schedulerProvider.ui())
                .doOnSuccess { teacherId ->
                    val info = ScheduleContainerInfo(teacherId, teacher.name, ScheduleType.STUDYGROUP)
                    view?.showNewScheduleContainerLoading(info)
                }
                .observeOn(schedulerProvider.single())
                .flatMap { insertWeeksWithWeekdays(it) }
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .doOnError { view?.showError() }
                .subscribeBy(
                        onSuccess = { teacherId ->
                            val info = ScheduleContainerInfo(teacherId, teacher.name, ScheduleType.TEACHER)
                            sharedPreferencesRepository.putSelectedScheduleContainer(info)
                            view?.initMainContent()
                            view?.addScheduleContainerMenuItem(info)
                            view?.checkScheduleContainerMenuItem(info)
                        },
                        onError = { throwable -> Timber.e(throwable) }
                )
    }

    private fun insertWeeksWithWeekdays(containerId: Int): Single<Int> {
        return networkRepository.get().getWeeks()
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
                .subscribeOn(schedulerProvider.single())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onNext = { scheduleContainer ->
                            val name = if (scheduleContainer.year != currentDate.academicYear) {
                                "(${scheduleContainer.year}) ${scheduleContainer.name}"
                            } else {
                                scheduleContainer.name
                            }
                            val info = ScheduleContainerInfo(scheduleContainer.id, name, scheduleContainer.type)
                            view?.addScheduleContainerMenuItem(info)
                        },
                        onError = { throwable -> Timber.e(throwable) },
                        onComplete = {
                            val info = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
                            view?.checkScheduleContainerMenuItem(info)
                        }
                )
    }

    fun isSelectedContainerNull() = sharedPreferencesRepository.getSelectedScheduleContainerInfo().type == null

    fun clearDisposables() = disposables.clear()
}
