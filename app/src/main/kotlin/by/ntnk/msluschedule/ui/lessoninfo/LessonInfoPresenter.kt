package by.ntnk.msluschedule.ui.lessoninfo

import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

data class ClassUiState(
    val groupClassDescriptor: StudyGroupLesson? = null,
    val teacherClassDescriptor: TeacherLesson? = null,
    val weekdays: List<String>? = null,
    val throwable: Throwable? = null
) {
    fun isGroupData(): Boolean = groupClassDescriptor != null && weekdays != null
    fun isTeacherData(): Boolean = teacherClassDescriptor != null && weekdays != null
    fun isError(): Boolean = throwable != null
}

class LessonInfoPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val schedulerProvider: SchedulerProvider
) : Presenter<LessonInfoView>() {

    private val classUiState: BehaviorSubject<ClassUiState> = BehaviorSubject.createDefault(ClassUiState())
    val classUiStateObservable: Observable<ClassUiState>
        get() = classUiState

    fun getClassDescriptor(classId: Int, scheduleType: ScheduleType, weekId: Int) {
        when (scheduleType) {
            ScheduleType.STUDYGROUP -> {
                databaseRepository.getStudyGroupLesson(classId)
                    .flatMap { classDescriptor ->
                        databaseRepository.getWeekdaysWithStudyGroupClassBySubject(weekId, classDescriptor)
                            .map { weekdays -> Pair(classDescriptor, weekdays) }
                    }
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .subscribeBy(
                        onSuccess = { (classDescriptor, weekdays) ->
                            val state = ClassUiState(groupClassDescriptor = classDescriptor, weekdays = weekdays)
                            classUiState.onNext(state)
                        },
                        onError = { throwable ->
                            val state = ClassUiState(throwable = throwable)
                            classUiState.onNext(state)
                        }
                    )
            }

            ScheduleType.TEACHER -> {
                databaseRepository.getTeacherLesson(classId)
                    .flatMap { classDescriptor ->
                        databaseRepository.getWeekdaysWithTeacherClassByGroups(weekId, classDescriptor.groups)
                            .map { weekdays -> Pair(classDescriptor, weekdays) }
                    }
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .subscribeBy(
                        onSuccess = { (classDescriptor, weekdays) ->
                            val state = ClassUiState(teacherClassDescriptor = classDescriptor, weekdays = weekdays)
                            classUiState.onNext(state)
                        },
                        onError = { throwable ->
                            val state = ClassUiState(throwable = throwable)
                            classUiState.onNext(state)
                        }
                    )
            }

            else -> {}
        }
    }
}
