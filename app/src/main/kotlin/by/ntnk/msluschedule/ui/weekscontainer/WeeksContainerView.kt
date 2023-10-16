package by.ntnk.msluschedule.ui.weekscontainer

import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.mvp.View
import by.ntnk.msluschedule.utils.Entry

interface WeeksContainerView : View {
    fun initWeeksAdapter(weekIds: List<Entry<Int, String>>, currentWeekItemIndex: Int)
    fun removeScheduleContainerFromView(info: ScheduleContainerInfo)
}
