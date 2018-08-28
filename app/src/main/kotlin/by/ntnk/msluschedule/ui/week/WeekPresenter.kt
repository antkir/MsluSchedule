package by.ntnk.msluschedule.ui.week

import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.db.DatabaseDataMapper
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

class WeekPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository,
        private val databaseDataMapper: DatabaseDataMapper,
        private val sharedPreferencesRepository: SharedPreferencesRepository,
        private val schedulerProvider: SchedulerProvider,
        private val networkRepository: NetworkRepository
) : Presenter<WeekView>() {
    private var scheduler = schedulerProvider.cachedThreadPool()

    fun getSchedule(weekId: Int) {
        val containerInfo = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        databaseRepository.getScheduleContainer(containerInfo.id)
                .flatMap { getScheduleData(it, weekId) }
                .subscribeOn(scheduler)
                .observeOn(schedulerProvider.ui())
                .subscribe(
                        { weekDayEntities -> view?.showSchedule(weekDayEntities) },
                        {
                            it.printStackTrace()
                            view?.showError(it, shouldSetupViews = true)
                        }
                )
    }

    private fun getScheduleData(container: ScheduleContainer, weekId: Int): Single<List<WeekdayWithLessons<Lesson>>> {
        return databaseRepository.isWeekInitialized(weekId)
                .flatMap { isInitialized ->
                    return@flatMap if (isInitialized) {
                        getWeekdaysWithLessonsForWeek(container.type, weekId)
                                .toList()
                    } else {
                        initSchedule(container, weekId)
                    }
                }
    }

    private fun getWeekdaysWithLessonsForWeek(type: ScheduleType, weekId: Int): Observable<WeekdayWithLessons<Lesson>> {
        return when (type) {
            ScheduleType.STUDYGROUP -> {
                databaseRepository.getWeekdayWithStudyGroupLessonsForWeek(weekId)
            }
            ScheduleType.TEACHER -> {
                databaseRepository.getWeekdayWithTeacherLessonsForWeek(weekId)
            }
        }
    }

    private fun initSchedule(container: ScheduleContainer, weekId: Int): Single<List<WeekdayWithLessons<Lesson>>> {
        return databaseRepository.insertWeekdays(weekId)
                .observeOn(schedulerProvider.ui())
                .doOnComplete { view?.showInitProgressBar() }
                .observeOn(scheduler)
                .andThen(downloadSchedule(container, weekId))
                .observeOn(schedulerProvider.ui())
                .doOnEvent { _, _ -> view?.hideInitProgressBar() }
    }

    private fun downloadSchedule(container: ScheduleContainer, weekId: Int): Single<List<WeekdayWithLessons<Lesson>>> {
        return when (container.type) {
            ScheduleType.STUDYGROUP -> {
                databaseRepository.getWeekKey(weekId)
                        .flatMapObservable {
                            val studyGroup = databaseDataMapper.mapToStudyGroup(container)
                            return@flatMapObservable networkRepository.getSchedule(studyGroup, it)
                        }
                        .toList()
                        .flatMap { databaseRepository.insertStudyGroupSchedule(it, weekId) }
            }
            ScheduleType.TEACHER -> {
                databaseRepository.getWeekKey(weekId)
                        .flatMapObservable {
                            val teacher = databaseDataMapper.mapToTeacher(container)
                            return@flatMapObservable networkRepository.getSchedule(teacher, it)
                        }
                        .toList()
                        .flatMap { databaseRepository.insertTeacherSchedule(it, weekId) }
            }
        }
    }

    fun updateSchedule(weekId: Int) {
        val containerInfo = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        databaseRepository.getScheduleContainer(containerInfo.id)
                .flatMap { scheduleContainer ->
                    databaseRepository.deleteLessonsForWeek(weekId, containerInfo.type!!)
                            .andThen(downloadSchedule(scheduleContainer, weekId))
                            .flatMap {
                                getWeekdaysWithLessonsForWeek(containerInfo.type, weekId)
                                        .toList()
                            }
                }
                .subscribeOn(schedulerProvider.cachedThreadPool())
                .observeOn(schedulerProvider.ui())
                .doOnSubscribe { view?.showUpdateProgressBar() }
                .doOnEvent { _, _ -> view?.hideUpdateProgressBar() }
                .doOnSuccess { _ -> view?.showUpdateSuccessMessage() }
                .subscribe(
                        { weekDayEntities -> view?.showSchedule(weekDayEntities) },
                        {
                            it.printStackTrace()
                            view?.showError(it, shouldSetupViews = false)
                        }
                )
    }
}
