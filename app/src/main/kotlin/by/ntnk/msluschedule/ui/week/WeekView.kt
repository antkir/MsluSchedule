package by.ntnk.msluschedule.ui.week

import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.mvp.View

interface WeekView : View {
    fun showSchedule(data: List<WeekdayWithLessons<Lesson>>)
    fun hideInitProgressBar()
    fun showInitProgressBar()
    fun hideUpdateProgressBar()
    fun showUpdateProgressBar()
    fun showUpdateSuccessMessage()
    fun showError(shouldSetupViews: Boolean)
}
