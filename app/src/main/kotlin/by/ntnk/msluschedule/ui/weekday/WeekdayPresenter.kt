package by.ntnk.msluschedule.ui.weekday

import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SchedulerProvider
import by.ntnk.msluschedule.utils.SharedPreferencesRepository
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

class WeekdayPresenter @Inject constructor(
    private val databaseRepository: DatabaseRepository,
    private val sharedPreferencesRepository: SharedPreferencesRepository,
    private val schedulerProvider: SchedulerProvider
) : Presenter<WeekdayView>() {

    fun initWeekdayView(weekdayId: Int) {
        val weekdayWithLessons = when (sharedPreferencesRepository.getSelectedScheduleContainerInfo().type) {
            ScheduleType.STUDYGROUP -> databaseRepository.getWeekdayWithStudyGroupLessons(weekdayId)
            ScheduleType.TEACHER -> databaseRepository.getWeekdayWithTeacherLessons(weekdayId)
            else -> throw NullPointerException()
        }

        databaseRepository.getNotesForWeekday(weekdayId)
            .toList()
            .zipWith(weekdayWithLessons) { notes, weekday -> Pair(weekday, notes) }
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { (weekday, notes) -> view?.initView(weekday, notes) },
                onError = { throwable -> Timber.e(throwable) }
            )
    }

    fun insertNote(note: Note, weekdayId: Int) {
        databaseRepository.insertNote(note, weekdayId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { id -> view?.addNote(Note(id, note.text, note.subject)) },
                onError = { throwable -> Timber.e(throwable) }
            )
    }

    fun deleteNote(id: Int) {
        databaseRepository.deleteNote(id)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onError = { throwable -> Timber.e(throwable) }
            )
    }

    fun updateNote(note: Note, weekdayId: Int) {
        databaseRepository.updateNote(note, weekdayId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onError = { throwable -> Timber.e(throwable) }
            )
    }

    fun restoreNote(note: Note, weekdayId: Int, color: Int, position: Int) {
        databaseRepository.insertNote(note, weekdayId)
            .subscribeOn(schedulerProvider.io())
            .observeOn(schedulerProvider.ui())
            .subscribeBy(
                onSuccess = { id -> view?.restoreNote(Note(id, note.text, note.subject), color, position) },
                onError = { throwable -> Timber.e(throwable) }
            )
    }
}
