package by.ntnk.msluschedule.ui.main

import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.mvp.View

interface MainView : View {
    fun changeScheduleContainer(scheduleContainer: ScheduleContainer)
    fun addScheduleContainerMenuItem(scheduleContainer: ScheduleContainer)
    fun selectScheduleContainerMenuItem(scheduleContainer: ScheduleContainer)
    fun showError()
}
