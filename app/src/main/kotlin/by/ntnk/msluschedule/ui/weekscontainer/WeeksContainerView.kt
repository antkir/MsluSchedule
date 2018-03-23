package by.ntnk.msluschedule.ui.weekscontainer

import by.ntnk.msluschedule.mvp.View
import by.ntnk.msluschedule.utils.ImmutableEntry

interface WeeksContainerView : View {
    fun initWeeksAdapter(weekIds: List<ImmutableEntry>, currentWeekItemIndex: Int)
}
