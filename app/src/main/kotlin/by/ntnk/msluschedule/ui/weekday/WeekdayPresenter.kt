package by.ntnk.msluschedule.ui.weekday

import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import by.ntnk.msluschedule.utils.assertDebug
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

data class WeekdayUiState(
    val isLoading: Boolean = false,
    val data: WeekdayWithLessons<Lesson>? = null,
    val throwable: Throwable? = null
) {
    fun isData(): Boolean = data != null
    fun isError(): Boolean = throwable != null
}

data class NotesUiState(
    val isLoading: Boolean = false,
    val data: List<Note>? = null,
    val throwable: Throwable? = null
) {
    fun isData(): Boolean = data != null
    fun isError(): Boolean = throwable != null
}

data class NoteChangeEvent(
    private var isInserted: Boolean = false,
    private var isDeleted: Boolean = false,
    private var isUpdated: Boolean = false,
    private var data: Note? = null,
    private var throwable: Throwable? = null
) {
    fun isInserted(): Boolean = isInserted

    fun isDataDeleted(): Boolean = data != null && isDeleted

    fun isUpdated(): Boolean = isUpdated

    fun consumeDataDeleted(): Note {
        isDeleted = false
        val note = data!!
        data = null
        return note
    }

    fun isError(): Boolean = throwable != null

    fun consumeError(): Throwable {
        isInserted = false
        isDeleted = false
        isUpdated = false
        val throwableCopy = throwable!!
        throwable = null
        return throwableCopy
    }
}

class WeekdayPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val schedulerProvider: SchedulerProvider
) : Presenter<WeekdayView>() {

    private val weekdayUiState: BehaviorSubject<WeekdayUiState> = BehaviorSubject.createDefault(WeekdayUiState())
    val weekdayUiStateObservable: Observable<WeekdayUiState>
        get() = weekdayUiState

    private val notesUiState: BehaviorSubject<NotesUiState> = BehaviorSubject.createDefault(NotesUiState())
    val notesUiStateObservable: Observable<NotesUiState>
        get() = notesUiState

    private val noteChangeEvent: BehaviorSubject<NoteChangeEvent> =
        BehaviorSubject.createDefault(NoteChangeEvent())
    val noteChangeEventObservable: Observable<NoteChangeEvent>
        get() = noteChangeEvent

    private var notesDisposable: Disposable = CompositeDisposable()

    fun getWeekday(id: Int) {
        weekdayUiState.onNext(WeekdayUiState(isLoading = true))
        when (sharedPreferencesRepository.getSelectedScheduleContainer().type) {
            ScheduleType.STUDYGROUP -> databaseRepository.getWeekdayWithStudyGroupLessons(id)
            ScheduleType.TEACHER -> databaseRepository.getWeekdayWithTeacherLessons(id)
            else -> Single.error(NullPointerException())
        }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { weekday -> weekdayUiState.onNext(WeekdayUiState(data = weekday)) },
                onError = { throwable -> weekdayUiState.onNext(WeekdayUiState(throwable = throwable)) }
            )
    }

    fun observeNotes(weekday: WeekdayWithLessons<Lesson>) {
        notesUiState.onNext(NotesUiState(isLoading = true))
        notesDisposable.dispose()
        notesDisposable = databaseRepository.getNotesForWeekday(weekday.id)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onNext = { notes -> notesUiState.onNext(NotesUiState(data = notes)) },
                onError = { throwable -> notesUiState.onNext(NotesUiState(throwable = throwable)) }
            )
    }

    fun insertNote(note: Note, weekdayId: Int) {
        databaseRepository.insertNote(note, weekdayId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onError = { throwable ->
                    noteChangeEvent.onNext(NoteChangeEvent(isInserted = true, throwable = throwable))
                }
            )
    }

    fun deleteNote(note: Note) {
        databaseRepository.deleteNote(note.id)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onComplete = { noteChangeEvent.onNext(NoteChangeEvent(isDeleted = true, data = note)) },
                onError = { throwable ->
                    noteChangeEvent.onNext(NoteChangeEvent(isDeleted = true, throwable = throwable))
                }
            )
    }

    fun updateNote(note: Note, weekdayId: Int) {
        databaseRepository.updateNote(note, weekdayId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onError = { throwable ->
                    noteChangeEvent.onNext(NoteChangeEvent(isUpdated = true, throwable = throwable))
                }
            )
    }

    fun isNotesUiStateDefault(): Boolean {
        return notesUiState.value == NotesUiState()
    }

    fun getWeekdayData(): WeekdayWithLessons<Lesson> {
        assertDebug { weekdayUiState.hasValue() }
        assertDebug { weekdayUiState.value!!.isData() }
        return weekdayUiState.value!!.data!!
    }

    fun destroy() {
        notesDisposable.dispose()
    }
}
