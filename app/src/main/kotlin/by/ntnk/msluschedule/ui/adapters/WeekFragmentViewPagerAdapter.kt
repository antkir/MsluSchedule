package by.ntnk.msluschedule.ui.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.view.ViewGroup
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

    fun getWeekFragment(index: Int): WeekFragment? {
        return if (index >= 0) fragments[index] else null
    }

    override fun getItem(position: Int): Fragment {
        val weekId = fragmentsInfo[position].key
        return WeekFragment.newInstance(weekId, isCurrentWeek = position == currentWeekIndex)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as WeekFragment
        fragments[position] = fragment
        return fragment
    }

    override fun getCount(): Int = fragmentsInfo.size

    override fun getPageTitle(position: Int): CharSequence? = fragmentsInfo[position].value
}
