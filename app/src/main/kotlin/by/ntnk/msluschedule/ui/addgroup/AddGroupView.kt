package by.ntnk.msluschedule.ui.addgroup

import by.ntnk.msluschedule.mvp.View

interface AddGroupView : View {
    fun showError(t: Throwable)
    fun populateFacultiesView(data: HashMap<Int, String>)
    fun populateGroupsView(data: HashMap<Int, String>)
}
