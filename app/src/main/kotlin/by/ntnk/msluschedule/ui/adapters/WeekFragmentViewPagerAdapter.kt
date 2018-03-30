package by.ntnk.msluschedule.ui.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import by.ntnk.msluschedule.ui.week.WeekFragment
import by.ntnk.msluschedule.utils.ImmutableEntry

class WeekFragmentViewPagerAdapter(
        fragmentManager: FragmentManager,
        private val fragmentsInfo: List<ImmutableEntry>
) : FragmentStatePagerAdapter(fragmentManager) {
    override fun getItem(position: Int): Fragment = WeekFragment()

    override fun getCount(): Int = fragmentsInfo.size

    override fun getPageTitle(position: Int): CharSequence? = fragmentsInfo[position].value
}
