package by.ntnk.msluschedule.db

import by.ntnk.msluschedule.data.*
import by.ntnk.msluschedule.db.data.*
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.ScheduleType
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

@PerApp
class DatabaseRepository @Inject constructor(
        private val appDatabase: AppDatabase,
        private val databaseDataMapper: DatabaseDataMapper
) {
    fun insertStudyGroup(studyGroup: StudyGroup): Single<Int> {
        return Single.just(studyGroup)
                .map { ScheduleContainer(it.key, it.name, ScheduleType.STUDYGROUP, it.year, it.faculty, it.course) }
                .flatMap { Single.fromCallable { appDatabase.scheduleContainerDao.insert(it) } }
                .map { it.toInt() }
    }

    fun insertTeacher(teacher: Teacher): Single<Int> {
        return Single.just(teacher)
                .map { ScheduleContainer(it.key, it.name, ScheduleType.TEACHER, it.year) }
                .flatMap { Single.fromCallable { appDatabase.scheduleContainerDao.insert(it) } }
                .map { it.toInt() }
    }

    fun deleteScheduleContainer(id: Int): Completable =
            Completable.fromCallable { appDatabase.scheduleContainerDao.delete(id) }

    fun getScheduleContainers(): Observable<ScheduleContainer> {
        return appDatabase.scheduleContainerDao.getScheduleContainers()
                .flatMapObservable { Observable.fromIterable(it) }
    }

    fun getScheduleContainer(id: Int): Single<ScheduleContainer> =
            appDatabase.scheduleContainerDao.getScheduleContainer(id)

    fun insertWeekdays(weekId: Int): Completable {
        return Single.just(weekId)
                .map { databaseDataMapper.createWeekdaysList(it) }
                .flatMapCompletable {
                    Completable.fromCallable { appDatabase.weekdayDao.insert(it) }
                }
    }

    fun getWeekday(weekdayId: Int) = appDatabase.weekdayDao.getWeekday(weekdayId)

    fun insertWeeksGetIds(data: ScheduleFilter, containerId: Int): Observable<Int> {
        return Single.just(databaseDataMapper.map(data, containerId))
                .map { appDatabase.weekDao.insert(it) }
                .flatMapObservable { Observable.fromIterable(it) }
                .map { it.toInt() }
    }

    fun getWeeksForContainer(id: Int): Observable<Week> {
        return appDatabase.weekDao.getWeeksForContainer(id)
                .flatMapObservable { Observable.fromIterable(it) }
    }

    fun getWeekKey(id: Int): Single<Int> = appDatabase.weekDao.getWeek(id).map { it.key }

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

    fun insertStudyGroupSchedule(weekdays: List<WeekdayWithStudyGroupLessons>,
                                 weekId: Int): Single<List<WeekdayWithStudyGroupLessons>> {
        return getWeekdaysForWeek(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .flatMapSingle { weekday ->
                    val weekdayWithLessons = weekdays.firstOrNull { it.weekday == weekday.value }
                    val lessons = weekdayWithLessons?.lessons ?: ArrayList()
                    return@flatMapSingle insertStudyGroupLessons(lessons, weekday.id)
                            .flatMap { Single.just(WeekdayWithStudyGroupLessons(weekday.id, weekday.value, it)) }
                }
                .toList()
    }

    private fun insertStudyGroupLessons(lessons: List<StudyGroupLesson>,
                                        weekdayId: Int): Single<List<StudyGroupLesson>> {
        return Observable.fromIterable(lessons)
                .map { lesson -> DbStudyGroupLesson(lesson, weekdayId) }
                .toList()
                .flatMap { dbLessons ->
                    Single.fromCallable { appDatabase.studyGroupLessonDao.insert(dbLessons) }
                            .flatMapObservable { ids ->
                                for (i in 0 until ids.size) {
                                    lessons[i].id = ids[i].toInt()
                                }
                                return@flatMapObservable Observable.fromIterable(lessons)
                            }
                            .toList()
                }
    }

    fun insertTeacherSchedule(weekdays: List<WeekdayWithTeacherLessons>,
                              weekId: Int): Single<List<WeekdayWithTeacherLessons>> {
        return getWeekdaysForWeek(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .flatMapSingle { weekday ->
                    val weekdayWithLessons = weekdays.firstOrNull { it.weekday == weekday.value }
                    val lessons = weekdayWithLessons?.lessons ?: ArrayList()
                    return@flatMapSingle insertTeacherLessons(lessons, weekday.id)
                            .flatMap { Single.just(WeekdayWithTeacherLessons(weekday.id, weekday.value, it)) }
                }
                .toList()
    }

    private fun insertTeacherLessons(lessons: List<TeacherLesson>, weekdayId: Int): Single<List<TeacherLesson>> {
        return Observable.fromIterable(lessons)
                .map { lesson -> DbTeacherLesson(lesson, weekdayId) }
                .toList()
                .flatMap { dbLessons ->
                    Single.fromCallable { appDatabase.teacherLessonDao.insert(dbLessons) }
                            .flatMapObservable { ids ->
                                for (i in 0 until ids.size) {
                                    lessons[i].id = ids[i].toInt()
                                }
                                return@flatMapObservable Observable.fromIterable(lessons)
                            }
                            .toList()
                }
    }

    fun deleteLessonsForWeek(weekId: Int, scheduleType: ScheduleType): Completable {
        return getWeekdaysForWeek(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .map { it.id }
                .flatMapCompletable {
                    when (scheduleType) {
                        ScheduleType.STUDYGROUP -> deleteStudyGroupLessons(it)
                        ScheduleType.TEACHER -> deleteTeacherLessons(it)
                    }
                }
    }

    private fun getWeekdaysForWeek(id: Int): Single<List<Weekday>> = appDatabase.weekdayDao.getWeekdaysForWeek(id)

    private fun deleteStudyGroupLessons(weekdayId: Int): Completable =
            Completable.fromCallable { appDatabase.studyGroupLessonDao.deleteForWeekday(weekdayId) }

    private fun deleteTeacherLessons(weekdayId: Int): Completable =
            Completable.fromCallable { appDatabase.teacherLessonDao.deleteForWeekday(weekdayId) }

    fun getNotesForWeekday(weekdayId: Int): Observable<Note> {
        return appDatabase.noteDao.getNotesForWeekday(weekdayId)
                .flatMapObservable { Observable.fromIterable(it) }
                .map { Note(it.id, it.text) }
    }

    fun insertNote(note: String, weekdayId: Int): Single<Int> {
        return Single.just(note)
                .map { DbNote(it, weekdayId) }
                .flatMap {
                    Single.fromCallable { appDatabase.noteDao.insert(it) }
                }
                .map { it.toInt() }
    }

    fun updateNote(note: Note, weekdayId: Int): Completable {
        return Single.just(note)
                .map { DbNote(it.text, weekdayId, it.id) }
                .flatMapCompletable {
                    Completable.fromCallable { appDatabase.noteDao.update(it) }
                }
    }

    fun deleteNote(id: Int): Completable = Completable.fromCallable { appDatabase.noteDao.delete(id) }
}
