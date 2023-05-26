package by.ntnk.msluschedule.ui.weekscontainer

import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.db.data.Week
import by.ntnk.msluschedule.mvp.View

interface WeeksContainerView : View {
    fun onData(weeks: List<Week>, currentAcademicWeekIndex: Int)
    fun onDeleteScheduleContainer(info: ScheduleContainerInfo)
}
