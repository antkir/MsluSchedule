package by.ntnk.msluschedule.ui.lessoninfo

import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.ErrorType
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.Utils
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

data class DataState(
    private val groupClass: StudyGroupLesson? = null,
    private val teacherClass: TeacherLesson? = null,
    private val weekdays: List<String>? = null
) {
    fun groupClassDataOrNull(): Pair<StudyGroupLesson, List<String>>? {
        if (groupClass == null || weekdays == null) {
            return null
        }
        return Pair(groupClass, weekdays)
    }

    fun teacherClassDataOrNull(): Pair<TeacherLesson, List<String>>? {
        if (teacherClass == null || weekdays == null) {
            return null
        }
        return Pair(teacherClass, weekdays)
    }
}

data class ErrorUiEvent(
    val error: ErrorType? = null
)

data class ClassInfoUiState(
    val isUiStateInitialized: Boolean = false,
    val dataState: DataState = DataState(),
    val errorEvent: ErrorUiEvent = ErrorUiEvent()
)

class LessonInfoPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val schedulerProvider: SchedulerProvider
) : Presenter<LessonInfoView>() {

    private val uiState: BehaviorSubject<ClassInfoUiState> = BehaviorSubject.createDefault(ClassInfoUiState())
    val uiStateObservable: Observable<ClassInfoUiState>
        get() = uiState

    private val disposables: CompositeDisposable = CompositeDisposable()

    fun getData(classId: Int, scheduleType: ScheduleType, weekId: Int) {
        when (scheduleType) {
            ScheduleType.STUDYGROUP -> {
                disposables += databaseRepository.getStudyGroupLesson(classId)
                    .flatMap { classData ->
                        databaseRepository.getWeekdaysWithStudyGroupClass(weekId, classData)
                            .map { weekdays -> Pair(classData, weekdays) }
                    }
                    .doOnError { AndroidUtils.logException(it) }
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .subscribeBy(
                        onSuccess = { (classData, weekdays) ->
                            uiState.onNext(
                                uiState.value!!.copy(
                                    dataState = DataState(
                                        groupClass = classData,
                                        weekdays = weekdays
                                    )
                                )
                            )
                        },
                        onError = { throwable ->
                            val error = Utils.getErrorType(throwable)
                            uiState.onNext(
                                uiState.value!!.copy(errorEvent = ErrorUiEvent(error = error))
                            )
                        }
                    )
            }

            ScheduleType.TEACHER -> {
                disposables += databaseRepository.getTeacherLesson(classId)
                    .flatMap { classData ->
                        databaseRepository.getWeekdaysWithTeacherClass(weekId, classData)
                            .map { weekdays -> Pair(classData, weekdays) }
                    }
                    .doOnError { AndroidUtils.logException(it) }
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .subscribeBy(
                        onSuccess = { (classData, weekdays) ->
                            uiState.onNext(
                                uiState.value!!.copy(
                                    dataState = DataState(
                                        teacherClass = classData,
                                        weekdays = weekdays
                                    )
                                )
                            )
                        },
                        onError = { throwable ->
                            val error = Utils.getErrorType(throwable)
                            uiState.onNext(
                                uiState.value!!.copy(errorEvent = ErrorUiEvent(error = error))
                            )
                        }
                    )
            }

            else -> {}
        }
    }

    fun errorEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(errorEvent = ErrorUiEvent())
        )
    }

    override fun onCleared() {
        disposables.clear()
    }
}
