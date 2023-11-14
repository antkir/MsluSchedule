package by.ntnk.msluschedule.ui.weekday

import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.ErrorType
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import by.ntnk.msluschedule.utils.Utils
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.subjects.BehaviorSubject
import javax.inject.Inject

data class DataState(
    val data: WeekdayWithLessons<Lesson>? = null
)

data class NotesState(
    val data: List<Note>? = null,
    val classes: List<Lesson>? = null
)

data class UiEvent(
    val isTriggered: Boolean = false
)

data class NoteUpdatedUiEvent(
    val isTriggered: Boolean = false,
    val error: ErrorType? = null
)

data class NoteDeletedUiEvent(
    val isTriggered: Boolean = false,
    val data: Note? = null,
    val error: ErrorType? = null
)

data class ErrorUiEvent(
    val error: ErrorType? = null
)

data class WeekdayUiState(
    val isUiStateInitialized: Boolean = false,
    val dataState: DataState = DataState(),
    val notesState: NotesState = NotesState(),
    val weekdayDataReadyEvent: UiEvent = UiEvent(),
    val noteUpdatedEvent: NoteUpdatedUiEvent = NoteUpdatedUiEvent(),
    val noteDeletedEvent: NoteDeletedUiEvent = NoteDeletedUiEvent(),
    val errorEvent: ErrorUiEvent = ErrorUiEvent()
)

class WeekdayPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val schedulerProvider: SchedulerProvider
) : Presenter<WeekdayView>() {

    private val uiState: BehaviorSubject<WeekdayUiState> = BehaviorSubject.createDefault(WeekdayUiState())
    val uiStateObservable: Observable<WeekdayUiState>
        get() = uiState

    private val disposables: CompositeDisposable = CompositeDisposable()

    fun getWeekday(id: Int) {
        disposables += when (sharedPreferencesRepository.getSelectedScheduleContainer().type) {
            ScheduleType.STUDYGROUP -> databaseRepository.getWeekdayWithStudyGroupLessons(id)
            ScheduleType.TEACHER -> databaseRepository.getWeekdayWithTeacherLessons(id)
            else -> Single.error(NullPointerException())
        }
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { weekday ->
                    uiState.onNext(
                        uiState.value!!.copy(
                            dataState = DataState(weekday),
                            weekdayDataReadyEvent = UiEvent(isTriggered = true)
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

    fun observeNotes(weekdayId: Int) {
        disposables += databaseRepository.getNotesForWeekdayReactive(weekdayId)
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onNext = { notes ->
                    val classes = uiState.value!!.dataState.data?.classes
                    uiState.onNext(
                        uiState.value!!.copy(notesState = NotesState(notes, classes))
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

    fun insertNote(note: Note, weekdayId: Int) {
        disposables += databaseRepository.insertNote(note, weekdayId)
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onError = { throwable ->
                    val error = Utils.getErrorType(throwable)
                    uiState.onNext(
                        uiState.value!!.copy(
                            noteUpdatedEvent = NoteUpdatedUiEvent(
                                isTriggered = true,
                                error = error
                            )
                        )
                    )
                }
            )
    }

    fun deleteNote(note: Note) {
        disposables += databaseRepository.deleteNote(note.id)
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onComplete = {
                    uiState.onNext(
                        uiState.value!!.copy(
                            noteDeletedEvent = NoteDeletedUiEvent(
                                isTriggered = true,
                                data = note
                            )
                        )
                    )
                },
                onError = { throwable ->
                    val error = Utils.getErrorType(throwable)
                    uiState.onNext(
                        uiState.value!!.copy(
                            noteDeletedEvent = NoteDeletedUiEvent(
                                isTriggered = true,
                                error = error
                            )
                        )
                    )
                }
            )
    }

    fun updateNote(note: Note, weekdayId: Int) {
        disposables += databaseRepository.updateNote(note, weekdayId)
            .doOnError { AndroidUtils.logException(it) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onError = { throwable ->
                    val error = Utils.getErrorType(throwable)
                    uiState.onNext(
                        uiState.value!!.copy(
                            noteUpdatedEvent = NoteUpdatedUiEvent(
                                isTriggered = true,
                                error = error
                            )
                        )
                    )
                }
            )
    }

    fun weekdayDataReadyEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(weekdayDataReadyEvent = UiEvent())
        )
    }

    fun noteUpdatedEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(noteUpdatedEvent = NoteUpdatedUiEvent())
        )
    }

    fun noteDeletedEventConsumed() {
        uiState.onNext(
            uiState.value!!.copy(noteDeletedEvent = NoteDeletedUiEvent())
        )
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
