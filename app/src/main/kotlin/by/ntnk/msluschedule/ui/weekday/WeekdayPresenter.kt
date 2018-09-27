package by.ntnk.msluschedule.ui.weekday

import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.db.DatabaseRepository
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.utils.SchedulerProvider
import io.reactivex.rxkotlin.subscribeBy
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
                        onError = { it.printStackTrace() }
                )
    }

    fun getNotes(weekdayId: Int) {
        databaseRepository.getNotesForWeekday(weekdayId)
                .toList()
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = { view?.showNotes(it) },
                        onError = { it.printStackTrace() }
                )
    }

    fun insertNote(note: String, weekdayId: Int) {
        databaseRepository.insertNote(note, weekdayId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = { view?.addNote(Note(it, note)) },
                        onError = { it.printStackTrace() }
                )
    }

    fun deleteNote(id: Int) {
        databaseRepository.deleteNote(id)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onError = { it.printStackTrace() }
                )
    }

    fun updateNote(note: Note, weekdayId: Int) {
        databaseRepository.updateNote(note, weekdayId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onError = { it.printStackTrace() }
                )
    }

    fun restoreNote(note: String, position: Int, color: Int, weekdayId: Int) {
        databaseRepository.insertNote(note, weekdayId)
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .subscribeBy(
                        onSuccess = { view?.restoreNote(Note(it, note), position, color) },
                        onError = { it.printStackTrace() }
                )
    }
}
