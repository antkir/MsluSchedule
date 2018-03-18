package by.ntnk.msluschedule.db

import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.data.Teacher
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
                .map { databaseDataMapper.createWeekDaysList(it) }
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
}
