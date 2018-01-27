package by.ntnk.msluschedule.ui.addteacher

import by.ntnk.msluschedule.mvp.View

interface AddTeacherView : View {
    fun showError(t: Throwable)
    fun populateTeachersView(data: HashMap<Int, String>)
}
