package by.ntnk.msluschedule.ui.week

import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.db.DatabaseDataMapper
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.CurrentDate
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.threeten.bp.DayOfWeek
import timber.log.Timber
import javax.inject.Inject

class WeekPresenter @Inject constructor(
        private val currentDate: CurrentDate,
        private val databaseRepository: DatabaseRepository,
        private val databaseDataMapper: DatabaseDataMapper,
        private val sharedPreferencesRepository: SharedPreferencesRepository,
        private val schedulerProvider: SchedulerProvider,
        private val networkRepository: dagger.Lazy<NetworkRepository>
) : Presenter<WeekView>() {
    private val disposables: CompositeDisposable = CompositeDisposable()
    private var scheduler = schedulerProvider.cachedThreadPool()

    fun getSchedule(weekId: Int, shouldUpdateAdapter: Boolean) {
        val containerInfo = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        disposables += databaseRepository.getScheduleContainer(containerInfo.id)
                .flatMap { getScheduleData(it, weekId) }
                .subscribeOn(scheduler)
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = {
                            if (shouldUpdateAdapter) {
                                view?.showSchedule(it)
                            }
                            for (weekday in it) {
                                getNotesStatus(weekday.weekdayId)
                            }
                        },
                        onError = {
                            Timber.i(it)
                            view?.showError(it, shouldSetupViews = true)
                        }
                )
    }

    private fun getNotesStatus(weekdayId: Int) {
        databaseRepository.getNotesForWeekday(weekdayId)
                .toList()
                .map { notes -> notes.isNotEmpty() }
                .subscribeOn(scheduler)
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = { isNotesListNotEmpty -> view?.updateNotesStatus(weekdayId, isNotesListNotEmpty) },
                        onError = { throwable -> Timber.e(throwable) }
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
                .andThen(downloadSchedule(isUpdate = false, container = container, weekId = weekId))
                .observeOn(schedulerProvider.ui())
                .doOnDispose { view?.hideUpdateProgressBar() }
                .doOnEvent { _, _ -> view?.hideInitProgressBar() }
    }

    private fun downloadSchedule(isUpdate: Boolean, container: ScheduleContainer,
                                 weekId: Int): Single<List<WeekdayWithLessons<Lesson>>> {
        return when (container.type) {
            ScheduleType.STUDYGROUP -> {
                databaseRepository.getWeekKey(weekId)
                        .flatMapObservable {
                            val studyGroup = databaseDataMapper.mapToStudyGroup(container)
                            return@flatMapObservable networkRepository.get().getSchedule(studyGroup, it)
                        }
                        .toList()
                        .flatMap { weekdaysWithLessons ->
                            return@flatMap if (isUpdate) {
                                databaseRepository.deleteLessonsForWeek(weekId, container.type)
                                        .andThen(Single.just(weekdaysWithLessons))
                            } else {
                                Single.just(weekdaysWithLessons)
                            }
                        }
                        .flatMap { weekdaysWithLessons ->
                            databaseRepository.insertStudyGroupSchedule(weekdaysWithLessons, weekId)
                        }
            }
            ScheduleType.TEACHER -> {
                databaseRepository.getWeekKey(weekId)
                        .flatMapObservable {
                            val teacher = databaseDataMapper.mapToTeacher(container)
                            return@flatMapObservable networkRepository.get().getSchedule(teacher, it)
                        }
                        .toList()
                        .flatMap { weekdaysWithLessons ->
                            return@flatMap if (isUpdate) {
                                databaseRepository.deleteLessonsForWeek(weekId, container.type)
                                        .andThen(Single.just(weekdaysWithLessons))
                            } else {
                                Single.just(weekdaysWithLessons)
                            }
                        }
                        .flatMap { weekdaysWithLessons ->
                            databaseRepository.insertTeacherSchedule(weekdaysWithLessons, weekId)
                        }
            }
        }
    }

    fun updateSchedule(weekId: Int) {
        val containerInfo = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        disposables += databaseRepository.getScheduleContainer(containerInfo.id)
                .flatMap { scheduleContainer ->
                    downloadSchedule(isUpdate = true, container = scheduleContainer, weekId = weekId)
                            .flatMap { getWeekdaysWithLessonsForWeek(containerInfo.type!!, weekId).toList() }
                }
                .subscribeOn(schedulerProvider.cachedThreadPool())
                .observeOn(schedulerProvider.ui())
                .doOnSubscribe { view?.showUpdateProgressBar() }
                .doOnDispose { view?.hideUpdateProgressBar() }
                .doOnEvent { _, _ -> view?.hideUpdateProgressBar() }
                .doOnSuccess { view?.showUpdateSuccessMessage() }
                .subscribeBy(
                        onSuccess = { weekDayEntities -> view?.showSchedule(weekDayEntities) },
                        onError = {
                            Timber.i(it)
                            view?.showError(it, shouldSetupViews = false)
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

    fun clearDisposables() = disposables.clear()
}
