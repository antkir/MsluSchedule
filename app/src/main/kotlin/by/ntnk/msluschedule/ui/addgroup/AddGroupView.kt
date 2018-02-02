package by.ntnk.msluschedule.ui.addgroup

import by.ntnk.msluschedule.mvp.View
import by.ntnk.msluschedule.network.data.ScheduleFilter

interface AddGroupView : View {
    fun showError(t: Throwable)
    fun populateFacultiesView(data: ScheduleFilter)
    fun populateGroupsView(data: ScheduleFilter)
}
