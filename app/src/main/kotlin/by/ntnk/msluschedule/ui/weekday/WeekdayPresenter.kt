package by.ntnk.msluschedule.ui.weekday

import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.SchedulerProvider
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

class WeekdayPresenter @Inject constructor(
        private val databaseRepository: DatabaseRepository,
        private val schedulerProvider: SchedulerProvider
) : Presenter<WeekdayView>() {
    fun getWeekday(weekdayId: Int) {
        databaseRepository.getWeekday(weekdayId)
                .map { it.value }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = { weekday -> view?.setToolbar(weekday) },
                        onError = { throwable -> Timber.e(throwable) }
                )
    }

    fun getNotes(weekdayId: Int) {
        databaseRepository.getNotesForWeekday(weekdayId)
                .toList()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = { view?.showNotes(it) },
                        onError = { throwable -> Timber.e(throwable) }
                )
    }

    fun insertNote(note: String, weekdayId: Int) {
        databaseRepository.insertNote(note, weekdayId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = { view?.addNote(Note(it, note)) },
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

    fun restoreNote(note: String, position: Int, color: Int, weekdayId: Int) {
        databaseRepository.insertNote(note, weekdayId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = { view?.restoreNote(Note(it, note), position, color) },
                        onError = { throwable -> Timber.e(throwable) }
                )
    }
}
