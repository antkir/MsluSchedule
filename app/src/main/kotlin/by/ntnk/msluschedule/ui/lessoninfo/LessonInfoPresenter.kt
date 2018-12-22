package by.ntnk.msluschedule.ui.lessoninfo

import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

class LessonInfoPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository,
        private val schedulerProvider: SchedulerProvider
) : Presenter<LessonInfoView>() {
    fun getLesson(lessonId: Int, scheduleType: ScheduleType, weekId: Int) {
        when (scheduleType) {
            ScheduleType.STUDYGROUP -> {
                databaseRepository.getStudyGroupLesson(lessonId)
                        .flatMap { lesson ->
                            databaseRepository.getWeekdaysWithStudyGroupLesson(lesson.subject, weekId)
                                    .map { Pair(lesson, it) }
                                    .toMaybe()
                        }
                        .subscribeOn(schedulerProvider.io())
                        .observeOn(schedulerProvider.ui())
                        .subscribeBy(
                                onSuccess = { view?.showInfo(it.first, it.second) },
                                onError = {
                                    throwable -> Timber.e(throwable)
                                    view?.destroyView()
                                },
                                onComplete = { view?.destroyView() }
                        )
            }
            ScheduleType.TEACHER -> {
                databaseRepository.getTeacherLesson(lessonId)
                        .flatMap { lesson ->
                            databaseRepository.getWeekdaysWithTeacherLesson(lesson.groups, weekId)
                                    .map { Pair(lesson, it) }
                                    .toMaybe()
                        }
                        .subscribeOn(schedulerProvider.io())
                        .observeOn(schedulerProvider.ui())
                        .subscribeBy(
                                onSuccess = { view?.showInfo(it.first, it.second) },
                                onError = {
                                    throwable -> Timber.e(throwable)
                                    view?.destroyView()
                                },
                                onComplete = { view?.destroyView() }
                        )
            }
        }
    }
}
