package by.ntnk.msluschedule.db

import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.Week
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.data.WeekdayWithStudyGroupLessons
import by.ntnk.msluschedule.data.WeekdayWithTeacherLessons
import by.ntnk.msluschedule.db.data.DbNote
import by.ntnk.msluschedule.db.data.DbStudyGroupLesson
import by.ntnk.msluschedule.db.data.DbTeacherLesson
import by.ntnk.msluschedule.db.data.ScheduleContainer
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
            .flatMap { scheduleContainer -> appDatabase.scheduleContainerDao.insert(scheduleContainer) }
            .map { id -> id.toInt() }
    }

    fun insertTeacher(teacher: Teacher): Single<Int> {
        return Single.just(teacher)
            .map { ScheduleContainer(it.key, it.name, ScheduleType.TEACHER, it.year) }
            .flatMap { scheduleContainer -> appDatabase.scheduleContainerDao.insert(scheduleContainer) }
            .map { id -> id.toInt() }
    }

    fun deleteScheduleContainer(id: Int): Completable {
        return appDatabase.scheduleContainerDao.delete(id)
    }

    fun getScheduleContainers(): Observable<ScheduleContainer> {
        return appDatabase.scheduleContainerDao.getScheduleContainers()
            .flatMapObservable { Observable.fromIterable(it) }
    }

    fun getScheduleContainer(id: Int): Single<ScheduleContainer> {
        return appDatabase.scheduleContainerDao.getScheduleContainer(id)
    }

    fun insertWeekdays(weekId: Int): Completable {
        return Single.just(databaseDataMapper.createWeekdayList(weekId))
            .flatMapCompletable { weekdays -> appDatabase.weekdayDao.insert(weekdays).ignoreElement() }
    }

    fun insertWeeksGetIds(data: ScheduleFilter, containerId: Int): Observable<Int> {
        return Single.just(databaseDataMapper.map(data, containerId))
            .flatMap { weeks -> appDatabase.weekDao.insert(weeks) }
            .flatMapObservable { Observable.fromIterable(it) }
            .map { id -> id.toInt() }
    }

    fun getWeeks(containerId: Int): Observable<Week> {
        return appDatabase.weekDao.getWeeks(containerId)
            .flatMapObservable { Observable.fromIterable(it) }
            .map { dbWeek -> Week(dbWeek.id, dbWeek.value) }
    }

    fun getWeekKey(id: Int): Single<Int> {
        return appDatabase.weekDao.getWeek(id)
            .map { week -> week.key }
    }

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
            .map { weekday -> databaseDataMapper.map(weekday) }
    }

    fun getWeekdayWithTeacherLessons(weekdayId: Int): Single<WeekdayWithLessons<Lesson>> {
        return appDatabase.teacherLessonDao.getWeekdayWithLessons(weekdayId)
            .map { weekday -> databaseDataMapper.map(weekday) }
    }

    fun insertStudyGroupSchedule(
        weekdays: List<WeekdayWithStudyGroupLessons>,
        weekId: Int
    ): Single<List<WeekdayWithStudyGroupLessons>> {
        return appDatabase.weekdayDao.getWeekdays(weekId)
            .flatMapObservable { Observable.fromIterable(it) }
            .flatMapSingle { weekday ->
                val classes = weekdays.firstOrNull { it.weekday == weekday.value }?.classes ?: emptyList()
                insertStudyGroupClasses(classes, weekday.id)
                    .flatMap { insertedClasses ->
                        Single.just(
                            WeekdayWithStudyGroupLessons(
                                weekday.id,
                                weekday.value,
                                insertedClasses.toMutableList()
                            )
                        )
                    }
            }
            .toList()
    }

    private fun insertStudyGroupClasses(
        classes: List<StudyGroupLesson>,
        weekdayId: Int
    ): Single<List<StudyGroupLesson>> {
        return Observable.fromIterable(classes)
            .map { classDescriptor -> DbStudyGroupLesson(classDescriptor, weekdayId) }
            .toList()
            .flatMap { dbClasses ->
                appDatabase.studyGroupLessonDao.insert(dbClasses)
                    .flatMapObservable { ids ->
                        for (i in ids.indices) {
                            classes[i].id = ids[i].toInt()
                        }
                        Observable.fromIterable(classes)
                    }
                    .toList()
            }
    }

    fun insertTeacherSchedule(
        weekdays: List<WeekdayWithTeacherLessons>,
        weekId: Int
    ): Single<List<WeekdayWithTeacherLessons>> {
        return appDatabase.weekdayDao.getWeekdays(weekId)
            .flatMapObservable { Observable.fromIterable(it) }
            .flatMapSingle { weekday ->
                val classes = weekdays.firstOrNull { it.weekday == weekday.value }?.classes ?: emptyList()
                insertTeacherClasses(classes, weekday.id)
                    .flatMap { insertedClasses ->
                        Single.just(
                            WeekdayWithTeacherLessons(
                                weekday.id,
                                weekday.value,
                                insertedClasses.toMutableList()
                            )
                        )
                    }
            }
            .toList()
    }

    private fun insertTeacherClasses(classes: List<TeacherLesson>, weekdayId: Int): Single<List<TeacherLesson>> {
        return Observable.fromIterable(classes)
            .map { classDesciptor -> DbTeacherLesson(classDesciptor, weekdayId) }
            .toList()
            .flatMap { dbClasses ->
                appDatabase.teacherLessonDao.insert(dbClasses)
                    .flatMapObservable { ids ->
                        for (i in ids.indices) {
                            classes[i].id = ids[i].toInt()
                        }
                        Observable.fromIterable(classes)
                    }
                    .toList()
            }
    }

    fun getStudyGroupLesson(id: Int): Single<StudyGroupLesson> {
        return appDatabase.studyGroupLessonDao.getLesson(id)
            .map { StudyGroupLesson(it.subject, it.type, it.teacher, it.classroom, it.startTime, it.endTime, it.id) }
    }

    fun getTeacherLesson(id: Int): Single<TeacherLesson> {
        return appDatabase.teacherLessonDao.getLesson(id)
            .map {
                TeacherLesson(
                    it.subject,
                    it.faculty,
                    it.groups,
                    it.type,
                    it.classroom,
                    it.startTime,
                    it.endTime,
                    it.id
                )
            }
    }

    fun getWeekdaysWithStudyGroupClassBySubject(weekId: Int, classDescriptor: StudyGroupLesson): Single<List<String>> {
        return appDatabase.weekdayDao.getWeekdays(weekId)
            .flatMapObservable { Observable.fromIterable(it) }
            .flatMapMaybe { weekday ->
                val subject = classDescriptor.subject
                val type = classDescriptor.type
                appDatabase.studyGroupLessonDao.getClassesBySubject(weekday.id, subject, type)
                    .filter { it.isNotEmpty() }
                    .map { weekday.value }
            }
            .toList()
    }

    fun getWeekdaysWithTeacherClassByGroups(weekId: Int, groups: String): Single<List<String>> {
        return appDatabase.weekdayDao.getWeekdays(weekId)
            .flatMapObservable { Observable.fromIterable(it) }
            .flatMapMaybe { weekday ->
                appDatabase.teacherLessonDao.getClassesByGroups(weekday.id, groups)
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
                    else -> Completable.complete()
                }
            }
    }

    fun getNotesForWeekday(weekdayId: Int): Observable<List<Note>> {
        return appDatabase.noteDao.getNotesForWeekday(weekdayId)
            .flatMap {
                Observable.fromIterable(it)
                    .map { note -> Note(note.id, note.text, note.subject) }
                    .toList()
                    .toObservable()
            }

    }

    fun checkWeekdayHasNotes(weekdayId: Int): Single<Boolean> {
        return appDatabase.noteDao.checkWeekdayHasNotes(weekdayId)
    }

    fun insertNote(note: Note, weekdayId: Int): Single<Int> {
        return Single.just(DbNote(note.text, note.subject, weekdayId, note.id))
            .flatMap { appDatabase.noteDao.insert(it) }
            .map { id -> id.toInt() }
    }

    fun updateNote(note: Note, weekdayId: Int): Completable {
        return Single.just(DbNote(note.text, note.subject, weekdayId, note.id))
            .flatMapCompletable { appDatabase.noteDao.update(it) }
    }

    fun deleteNote(id: Int): Completable {
        return appDatabase.noteDao.delete(id)
    }
}
