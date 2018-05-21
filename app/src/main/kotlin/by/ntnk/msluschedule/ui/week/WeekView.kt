package by.ntnk.msluschedule.ui.week

import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.mvp.View

interface WeekView : View {
    fun showSchedule(data: List<WeekdayWithLessons<Lesson>>)
}
