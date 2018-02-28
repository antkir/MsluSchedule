package by.ntnk.msluschedule.ui.addteacher

import by.ntnk.msluschedule.mvp.View
import by.ntnk.msluschedule.network.data.ScheduleFilter

interface AddTeacherView : View {
    fun showError(t: Throwable)
    fun populateTeachersView(data: ScheduleFilter)
}
