package by.ntnk.msluschedule.ui.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.view.PagerAdapter
import by.ntnk.msluschedule.ui.week.WeekFragment
import by.ntnk.msluschedule.utils.ImmutableEntry

class WeekFragmentViewPagerAdapter(
        fragmentManager: FragmentManager,
        private var fragmentsInfo: List<ImmutableEntry>,
        private var currentWeekIndex: Int
) : FragmentStatePagerAdapter(fragmentManager) {

    fun swapData(fragmentsInfo: List<ImmutableEntry>, currentWeekIndex: Int): Boolean {
        if (this.fragmentsInfo != fragmentsInfo) {
            this.fragmentsInfo = fragmentsInfo
            this.currentWeekIndex = currentWeekIndex
            notifyDataSetChanged()
            return true
        }
        return false
    }

    override fun getFragment(position: Int): WeekFragment? {
        return super.getFragment(position) as WeekFragment?
    }

    override fun getItem(position: Int): Fragment {
        val weekId = fragmentsInfo[position].key
        return WeekFragment.newInstance(weekId, isCurrentWeek = position == currentWeekIndex)
    }

    override fun getItemPosition(any: Any) = PagerAdapter.POSITION_NONE

    override fun getCount(): Int = fragmentsInfo.size

    override fun getPageTitle(position: Int): CharSequence? = fragmentsInfo[position].value
}
