package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.mvp.View

interface MainView : View {
    fun initMainContent()
    fun addScheduleContainerMenuItem(scheduleContainer: ScheduleContainer)
    fun checkScheduleContainerMenuItem(scheduleContainer: ScheduleContainer)
    fun showNewScheduleContainerLoading(scheduleContainer: ScheduleContainer)
    fun showError()
}
