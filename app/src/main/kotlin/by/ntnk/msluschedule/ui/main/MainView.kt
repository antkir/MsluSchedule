package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.mvp.View

interface MainView : View {
    fun initMainContent()
    fun addScheduleContainerMenuItem(scheduleContainerInfo: ScheduleContainerInfo)
    fun checkScheduleContainerMenuItem(scheduleContainerInfo: ScheduleContainerInfo)
    fun showNewScheduleContainerLoading(scheduleContainerInfo: ScheduleContainerInfo)
    fun showError()
}
