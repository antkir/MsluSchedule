package by.ntnk.msluschedule.ui.adapters

import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.view.PagerAdapter
import by.ntnk.msluschedule.ui.week.WeekFragment
import by.ntnk.msluschedule.utils.ImmutableEntry

private const val ARG_WEEK_ID = "weekId"

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

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        state as Bundle?
        var s = state
        if (fragmentsInfo.isNotEmpty() && state?.getInt(ARG_WEEK_ID) != fragmentsInfo[currentWeekIndex].key) {
            s = null
        }
        super.restoreState(s, loader)
    }

    override fun saveState(): Parcelable? {
        var superState = super.saveState() as Bundle?
        if (superState == null) {
            superState = Bundle()
        }
        superState.putInt(ARG_WEEK_ID, fragmentsInfo[currentWeekIndex].key)
        return superState
    }
}
