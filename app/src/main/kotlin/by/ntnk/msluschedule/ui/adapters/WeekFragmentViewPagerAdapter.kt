package by.ntnk.msluschedule.ui.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import by.ntnk.msluschedule.ui.week.WeekFragment
import by.ntnk.msluschedule.utils.ImmutableEntry

class WeekFragmentViewPagerAdapter(
        fragmentManager: FragmentManager,
        private val fragmentsInfo: List<ImmutableEntry>,
        private val currentWeekIndex: Int
) : FragmentStatePagerAdapter(fragmentManager) {
    private val fragments: MutableList<WeekFragment?> = ArrayList()

    init {
        fragmentsInfo.forEach { fragments.add(null) }
    }

    fun getWeekFragment(index: Int): WeekFragment? = fragments[index]

    override fun getItem(position: Int): Fragment {
        val weekId = fragmentsInfo[position].key
        val fragment = WeekFragment.newInstance(weekId, isCurrentWeek = position == currentWeekIndex)
        fragments[position] = fragment
        return fragment
    }

    override fun getCount(): Int = fragmentsInfo.size

    override fun getPageTitle(position: Int): CharSequence? = fragmentsInfo[position].value
}
