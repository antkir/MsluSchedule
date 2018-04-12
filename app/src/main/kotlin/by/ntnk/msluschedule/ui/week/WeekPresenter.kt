package by.ntnk.msluschedule.ui.week

import by.ntnk.msluschedule.data.*
import by.ntnk.msluschedule.db.DatabaseDataMapper
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.db.data.ScheduleContainer
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.network.NetworkRepository
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

class WeekPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository,
        private val databaseDataMapper: DatabaseDataMapper,
        private val sharedPreferencesRepository: SharedPreferencesRepository,
        private val schedulerProvider: SchedulerProvider,
        private val networkRepository: NetworkRepository
) : Presenter<WeekView>() {
    fun getScheduleData(weekId: Int) {
        val containerInfo = sharedPreferencesRepository.getSelectedScheduleContainerInfo()
        databaseRepository.getScheduleContainer(containerInfo.id)
                .flatMap { getSchedule(it, weekId) }
                .subscribeOn(schedulerProvider.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { weekDayEntities -> Timber.d(weekDayEntities.toString()) },
                        { it.printStackTrace() })
    }

    private fun getSchedule(
            container: ScheduleContainer,
            weekId: Int
    ): Single<List<WeekdayWithLessons<Lesson>>> {
        return databaseRepository.isWeekInitialized(weekId)
                .flatMap { isInitialized ->
                    return@flatMap if (isInitialized) {
                        getWeekdaysWithLessonsForWeek(container.type, weekId)
                                .toList()
                    } else {
                        initWeekdaysAndSchedule(container, weekId)
                    }
                }

    }

    private fun getWeekdaysWithLessonsForWeek(
            type: ScheduleType,
            weekId: Int
    ): Observable<WeekdayWithLessons<Lesson>> {
        return when (type) {
            ScheduleType.STUDYGROUP -> {
                databaseRepository.getWeekdayWithStudyGroupLessonsForWeek(weekId)
            }
            ScheduleType.TEACHER -> {
                databaseRepository.getWeekdayWithTeacherLessonsForWeek(weekId)
            }
        }
    }

    private fun initWeekdaysAndSchedule(
            container: ScheduleContainer,
            weekId: Int
    ): Single<List<WeekdayWithLessons<Lesson>>> {
        return when (container.type) {
            ScheduleType.STUDYGROUP -> {
                databaseRepository.getWeekKey(weekId)
                        .flatMapObservable {
                            val studyGroup = databaseDataMapper.mapToStudyGroup(container)
                            return@flatMapObservable networkRepository.getSchedule(studyGroup, it) }
                        .toList()
                        .flatMap {
                            databaseRepository.insertWeekdays(weekId)
                                    .andThen(databaseRepository.insertStudyGroupSchedule(it, weekId))
                        }
            }
            ScheduleType.TEACHER -> {
                databaseRepository.getWeekKey(weekId)
                        .flatMapObservable {
                            val teacher = databaseDataMapper.mapToTeacher(container)
                            return@flatMapObservable networkRepository.getSchedule(teacher, it)
                        }
                        .toList()
                        .flatMap {
                            databaseRepository.insertWeekdays(weekId)
                                    .andThen(databaseRepository.insertTeacherSchedule(it, weekId))
                        }
            }
        }
    }
}
