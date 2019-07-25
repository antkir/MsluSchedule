package by.ntnk.msluschedule.ui.weekday

import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.mvp.View

interface WeekdayView : View {
    fun initView(weekdayWithLessons: WeekdayWithLessons<Lesson>, data: List<Note>)
    fun addNote(note: Note)
}
