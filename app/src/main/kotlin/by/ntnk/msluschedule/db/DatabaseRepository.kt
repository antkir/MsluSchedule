package by.ntnk.msluschedule.db

import by.ntnk.msluschedule.data.*
import by.ntnk.msluschedule.db.data.*
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.data.ScheduleFilter
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

@PerApp
class DatabaseRepository @Inject constructor(
        private val appDatabase: AppDatabase,
        private val databaseDataMapper: DatabaseDataMapper
) {
    fun insertStudyGroup(entry: StudyGroup): Single<Int> {
        return Single.just(entry)
                .map { databaseDataMapper.map(it) }
                .flatMap { Single.fromCallable { appDatabase.scheduleContainerDao.insert(it) } }
                .map { it.toInt() }
    }

    fun insertTeacher(entry: Teacher): Single<Int> {
        return Single.just(entry)
                .map { databaseDataMapper.map(it) }
                .flatMap { Single.fromCallable { appDatabase.scheduleContainerDao.insert(it) } }
                .map { it.toInt() }
    }

    fun insertWeekdays(weekId: Int): Completable {
        return Single.just(weekId)
                .map { databaseDataMapper.createWeekdaysList(it) }
                .flatMapCompletable {
                    Completable.fromCallable { appDatabase.weekdayDao.insert(it) }
                }
    }

    fun insertWeeksGetIds(data: ScheduleFilter, containerId: Int): Observable<Int> {
        return Single.just(databaseDataMapper.map(data, containerId))
                .map { appDatabase.weekDao.insert(it) }
                .flatMapObservable { Observable.fromIterable(it) }
                .map { it.toInt() }
    }

    private fun insertStudyGroupLessons(lessons: List<StudyGroupLesson>, weekdayId: Int): Completable {
        return Observable.fromIterable(lessons)
                .map { lesson -> DbStudyGroupLesson(lesson, weekdayId) }
                .toList()
                .flatMapCompletable {
                    Completable.fromCallable { appDatabase.studyGroupLessonDao.insert(it) }
                }
    }

    private fun insertTeacherLessons(lessons: List<TeacherLesson>, weekdayId: Int): Completable {
        return Observable.fromIterable(lessons)
                .map { lesson -> DbTeacherLesson(lesson, weekdayId) }
                .toList()
                .flatMapCompletable {
                    Completable.fromCallable { appDatabase.teacherLessonDao.insert(it) }
                }
    }

    fun deleteScheduleContainer(id: Int): Completable =
            Completable.fromCallable { appDatabase.scheduleContainerDao.delete(id) }

    fun getScheduleContainers(): Observable<ScheduleContainer> {
        return appDatabase.scheduleContainerDao.getScheduleContainers()
                .flatMapObservable { Observable.fromIterable(it) }
    }

    fun getScheduleContainer(id: Int): Single<ScheduleContainer> =
            appDatabase.scheduleContainerDao.getScheduleContainer(id)

    fun getWeeksForContainer(id: Int): Observable<Week> {
        return appDatabase.weekDao.getWeeksForContainer(id)
                .flatMapObservable { Observable.fromIterable(it) }
    }

    fun getWeekKey(id: Int): Single<Int> = appDatabase.weekDao.getWeek(id).map { it.key }

    private fun getWeekdaysForWeek(id: Int): Single<List<Weekday>> =
            appDatabase.weekdayDao.getWeekdaysForWeek(id)

    fun isWeekInitialized(weekId: Int): Single<Boolean> {
        return appDatabase.weekDao.getWeek(weekId)
                .flatMap { appDatabase.weekdayDao.getWeekdaysForWeek(it.id) }
                .map { weekDays -> weekDays.isNotEmpty() }
    }

    fun getWeekdayWithStudyGroupLessonsForWeek(weekId: Int): Observable<WeekdayWithLessons<Lesson>> {
        return appDatabase.studyGroupLessonDao.getWeekdayWithStudyGroupLessonsForWeek(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .map { databaseDataMapper.map(it) }
    }

    fun getWeekdayWithTeacherLessonsForWeek(weekId: Int): Observable<WeekdayWithLessons<*>> {
        return appDatabase.teacherLessonDao.getWeekdayWithTeacherLessonsForWeek(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .map { databaseDataMapper.map(it) }
    }

    fun insertStudyGroupSchedule(
            weekdays: List<WeekdayWithStudyGroupLessons>, weekId: Int
    ): Single<List<WeekdayWithStudyGroupLessons>> {
        return Observable.fromIterable(weekdays)
                .flatMapCompletable {
                    weekdayWithLessons -> getWeekdaysForWeek(weekId)
                        .flatMapObservable { Observable.fromIterable(it) }
                        .filter { weekday -> weekday.value == weekdayWithLessons.weekday }
                        .firstOrError()
                        .map { it.id }
                        .flatMapCompletable {
                            insertStudyGroupLessons(weekdayWithLessons.lessons, it)
                        }
                }
                .andThen(Single.just(weekdays))
    }

    fun insertTeacherSchedule(
            weekdays: List<WeekdayWithTeacherLessons>, weekId: Int
    ): Single<List<WeekdayWithTeacherLessons>> {
        return Observable.fromIterable(weekdays)
                .flatMapCompletable {
                    weekdayWithLessons -> getWeekdaysForWeek(weekId)
                        .flatMapObservable { Observable.fromIterable(it) }
                        .filter { weekday -> weekday.value == weekdayWithLessons.weekday }
                        .firstOrError()
                        .map { it.id }
                        .flatMapCompletable {
                            insertTeacherLessons(weekdayWithLessons.lessons, it)
                        }
                }
                .andThen(Single.just(weekdays))
    }
}
