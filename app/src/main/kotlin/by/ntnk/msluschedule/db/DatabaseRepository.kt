package by.ntnk.msluschedule.db

import by.ntnk.msluschedule.data.*
import by.ntnk.msluschedule.db.data.*
import by.ntnk.msluschedule.di.PerApp
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.utils.ScheduleType
import io.reactivex.Completable
import io.reactivex.Maybe
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
                .flatMap { appDatabase.scheduleContainerDao.insert(it) }
                .map { it.toInt() }
    }

    fun insertTeacher(teacher: Teacher): Single<Int> {
        return Single.just(teacher)
                .map { ScheduleContainer(teacher.key, teacher.name, ScheduleType.TEACHER, teacher.year) }
                .flatMap { appDatabase.scheduleContainerDao.insert(it) }
                .map { it.toInt() }
    }

    fun deleteScheduleContainer(id: Int): Completable = appDatabase.scheduleContainerDao.delete(id)

    fun getScheduleContainers(): Observable<ScheduleContainer> {
        return appDatabase.scheduleContainerDao.getScheduleContainers()
                .flatMapObservable { Observable.fromIterable(it) }
    }

    fun getScheduleContainer(id: Int): Single<ScheduleContainer> =
            appDatabase.scheduleContainerDao.getScheduleContainer(id)

    fun insertWeekdays(weekId: Int): Completable {
        return Single.just(databaseDataMapper.createWeekdayList(weekId))
                .flatMapCompletable { appDatabase.weekdayDao.insert(it).ignoreElement() }
    }

    fun insertWeeksGetIds(data: ScheduleFilter, containerId: Int): Observable<Int> {
        return Single.just(databaseDataMapper.map(data, containerId))
                .flatMap { appDatabase.weekDao.insert(it) }
                .flatMapObservable { Observable.fromIterable(it) }
                .map { it.toInt() }
    }

    fun getWeeks(containerId: Int): Observable<Week> {
        return appDatabase.weekDao.getWeeks(containerId)
                .flatMapObservable { Observable.fromIterable(it) }
    }

    fun getWeekKey(id: Int): Single<Int> = appDatabase.weekDao.getWeek(id).map { it.key }

    fun isWeekInitialized(weekId: Int): Single<Boolean> {
        return appDatabase.weekdayDao.getWeekdays(weekId)
                .map { weekdays -> weekdays.isNotEmpty() }
    }

    fun getWeekdaysWithStudyGroupLessons(weekId: Int): Observable<WeekdayWithLessons<Lesson>> {
        return appDatabase.studyGroupLessonDao.getWeekdaysWithLessons(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .map { databaseDataMapper.map(it) }
    }

    fun getWeekdaysWithTeacherLessons(weekId: Int): Observable<WeekdayWithLessons<*>> {
        return appDatabase.teacherLessonDao.getWeekdaysWithLessons(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .map { databaseDataMapper.map(it) }
    }

    fun getWeekdayWithStudyGroupLessons(weekdayId: Int): Single<WeekdayWithLessons<Lesson>> {
        return appDatabase.studyGroupLessonDao.getWeekdayWithLessons(weekdayId)
                .map { databaseDataMapper.map(it) }
    }

    fun getWeekdayWithTeacherLessons(weekdayId: Int): Single<WeekdayWithLessons<Lesson>> {
        return appDatabase.teacherLessonDao.getWeekdayWithLessons(weekdayId)
                .map { databaseDataMapper.map(it) }
    }

    fun insertStudyGroupSchedule(weekdays: List<WeekdayWithStudyGroupLessons>,
                                 weekId: Int): Single<List<WeekdayWithStudyGroupLessons>> {
        return appDatabase.weekdayDao.getWeekdays(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .flatMapSingle { weekday ->
                    val weekdayWithLessons = weekdays.firstOrNull { it.weekday == weekday.value }
                    val lessons = weekdayWithLessons?.lessons ?: emptyList<StudyGroupLesson>()
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
                    appDatabase.studyGroupLessonDao.insert(dbLessons)
                            .flatMapObservable { ids ->
                                for (i in ids.indices) {
                                    lessons[i].id = ids[i].toInt()
                                }
                                return@flatMapObservable Observable.fromIterable(lessons)
                            }
                            .toList()
                }
    }

    fun insertTeacherSchedule(weekdays: List<WeekdayWithTeacherLessons>,
                              weekId: Int): Single<List<WeekdayWithTeacherLessons>> {
        return appDatabase.weekdayDao.getWeekdays(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .flatMapSingle { weekday ->
                    val weekdayWithLessons = weekdays.firstOrNull { it.weekday == weekday.value }
                    val lessons = weekdayWithLessons?.lessons ?: emptyList<TeacherLesson>()
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
                    appDatabase.teacherLessonDao.insert(dbLessons)
                            .flatMapObservable { ids ->
                                for (i in ids.indices) {
                                    lessons[i].id = ids[i].toInt()
                                }
                                return@flatMapObservable Observable.fromIterable(lessons)
                            }
                            .toList()
                }
    }

    fun getStudyGroupLesson(id: Int): Maybe<StudyGroupLesson> {
        return appDatabase.studyGroupLessonDao.getLesson(id)
                .map { StudyGroupLesson(it.subject, it.teacher, it.classroom, it.startTime, it.endTime, it.id) }
    }

    fun getTeacherLesson(id: Int): Maybe<TeacherLesson> {
        return appDatabase.teacherLessonDao.getLesson(id)
                .map {
                    TeacherLesson(it.subject, it.faculty, it.groups, it.type,
                                  it.classroom, it.startTime, it.endTime, it.id)
                }
    }

    fun getWeekdaysWithStudyGroupLesson(lessonSubject: String, weekId: Int): Single<List<String>> {
        return appDatabase.weekdayDao.getWeekdays(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .flatMapMaybe { weekday ->
                    return@flatMapMaybe appDatabase.studyGroupLessonDao.getLessons(weekday.id, lessonSubject)
                            .filter { it.isNotEmpty() }
                            .map { weekday.value }
                }
                .toList()
    }

    fun getWeekdaysWithTeacherLesson(lessonGroups: String, weekId: Int): Single<List<String>> {
        return appDatabase.weekdayDao.getWeekdays(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .flatMapMaybe { weekday ->
                    return@flatMapMaybe appDatabase.teacherLessonDao.getLessons(weekday.id, lessonGroups)
                            .filter { it.isNotEmpty() }
                            .map { weekday.value }
                }
                .toList()
    }

    fun deleteLessons(weekId: Int, scheduleType: ScheduleType): Completable {
        return appDatabase.weekdayDao.getWeekdays(weekId)
                .flatMapObservable { Observable.fromIterable(it) }
                .map { weekday -> weekday.id }
                .flatMapCompletable { weekdayId ->
                    when (scheduleType) {
                        ScheduleType.STUDYGROUP -> appDatabase.studyGroupLessonDao.delete(weekdayId)
                        ScheduleType.TEACHER -> appDatabase.teacherLessonDao.delete(weekdayId)
                    }
                }
    }

    fun getNotesForWeekday(weekdayId: Int): Observable<Note> {
        return appDatabase.noteDao.getNotesForWeekday(weekdayId)
                .flatMapObservable { Observable.fromIterable(it) }
                .map { Note(it.id, it.text, it.subject) }
    }

    fun insertNote(note: Note, weekdayId: Int): Single<Int> {
        return Single.just(DbNote(note.text, note.subject, weekdayId))
                .flatMap { appDatabase.noteDao.insert(it) }
                .map { it.toInt() }
    }

    fun updateNote(note: Note, weekdayId: Int): Completable {
        return Single.just(DbNote(note.text, note.subject, weekdayId, note.id))
                .flatMapCompletable { appDatabase.noteDao.update(it) }
    }

    fun deleteNote(id: Int): Completable = appDatabase.noteDao.delete(id)
}
