package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.mvp.View

interface MainView : View {
    fun initMainContent()
    fun addScheduleContainerMenuItem(scheduleContainer: ScheduleContainerInfo)
    fun checkScheduleContainerMenuItem(scheduleContainer: ScheduleContainerInfo)
    fun showNewScheduleContainerLoading(scheduleContainer: ScheduleContainerInfo)
    fun showError()
}
