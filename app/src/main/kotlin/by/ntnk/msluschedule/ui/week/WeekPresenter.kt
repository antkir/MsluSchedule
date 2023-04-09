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
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.DayOfWeek
import javax.inject.Inject

class WeekPresenter @Inject constructor(
    private val currentDate: CurrentDate,
    private val databaseRepository: DatabaseRepository,
    private val databaseDataMapper: DatabaseDataMapper,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val schedulerProvider: SchedulerProvider,
    private val networkRepository: dagger.Lazy<NetworkRepository>
) : Presenter<WeekView>() {

    fun getScheduleContainerInfo(): ScheduleContainerInfo {
        return sharedPreferencesRepository.getSelectedScheduleContainerInfo()
    }

    fun getSchedule(scheduleType: ScheduleType, weekId: Int): Single<List<WeekdayWithLessons<Lesson>>> {
        return getWeekdays(scheduleType, weekId)
            .toList()
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
    }

    private fun getWeekdays(type: ScheduleType, weekId: Int): Observable<WeekdayWithLessons<Lesson>> {
        return when (type) {
            ScheduleType.STUDYGROUP -> {
                databaseRepository.getWeekdaysWithStudyGroupLessons(weekId)
            }
            ScheduleType.TEACHER -> {
                databaseRepository.getWeekdaysWithTeacherLessons(weekId)
            }
        }
    }

    fun getNotesStatus(weekdayId: Int): Single<Boolean> {
        return databaseRepository.getNotesForWeekday(weekdayId)
            .toList()
            .map { notes -> notes.isNotEmpty() }
            .subscribeOn(schedulerProvider.single())
            .observeOn(schedulerProvider.ui())
    }

    fun initSchedule(weekId: Int): Completable {
        return databaseRepository.insertWeekdays(weekId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
    }

    fun updateSchedule(scheduleContainerId: Int, weekId: Int): Single<List<WeekdayWithLessons<Lesson>>> {
        return databaseRepository.getScheduleContainer(scheduleContainerId)
            .flatMap { scheduleContainer ->
                when (scheduleContainer.type) {
                    ScheduleType.STUDYGROUP -> {
                        databaseRepository.getWeekKey(weekId)
                            .flatMapObservable {
                                val studyGroup = databaseDataMapper.mapToStudyGroup(scheduleContainer)
                                return@flatMapObservable networkRepository.get().getSchedule(studyGroup, it)
                            }
                            .toList()
                            .flatMap { weekdays ->
                                databaseRepository.deleteLessons(weekId, scheduleContainer.type)
                                    .andThen(Single.just(weekdays))
                            }
                            .flatMap { weekdays ->
                                databaseRepository.insertStudyGroupSchedule(weekdays, weekId)
                            }
                    }
                    ScheduleType.TEACHER -> {
                        databaseRepository.getWeekKey(weekId)
                            .flatMapObservable {
                                val teacher = databaseDataMapper.mapToTeacher(scheduleContainer)
                                return@flatMapObservable networkRepository.get().getSchedule(teacher, it)
                            }
                            .toList()
                            .flatMap { weekdays ->
                                databaseRepository.deleteLessons(weekId, scheduleContainer.type)
                                    .andThen(Single.just(weekdays))
                            }
                            .flatMap { weekdays ->
                                databaseRepository.insertTeacherSchedule(weekdays, weekId)
                            }
                    }
                }
            }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
    }

    fun isWeekInitialized(weekId: Int): Single<Boolean> {
        return databaseRepository.isWeekInitialized(weekId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
    }

    fun getCurrentDayOfWeek(): Int {
        return if (currentDate.academicWeek >= 0) {
            currentDate.date.dayOfWeek.value
        } else {
            DayOfWeek.MONDAY.value
        }
    }
}
