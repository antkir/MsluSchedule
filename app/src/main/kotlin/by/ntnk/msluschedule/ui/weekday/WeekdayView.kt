package by.ntnk.msluschedule.ui.weekday

import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.mvp.View

interface WeekdayView : View {
    fun setToolbar(weekday: String)
    fun showNotes(data: List<Note>)
    fun addNote(note: Note)
}
