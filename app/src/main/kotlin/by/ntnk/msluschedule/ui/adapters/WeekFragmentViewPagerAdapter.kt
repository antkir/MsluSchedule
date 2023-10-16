package by.ntnk.msluschedule.ui.adapters

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import by.ntnk.msluschedule.ui.week.WeekFragment
import by.ntnk.msluschedule.utils.Entry

private const val ARG_WEEK_ID = "weekId"

class WeekFragmentViewPagerAdapter(
    fragmentManager: FragmentManager,
    private var fragmentsInfo: List<Entry<Int, String>>,
    private var currentWeekIndex: Int
) : FragmentStatePagerAdapter(fragmentManager) {

    fun swapData(fragmentsInfo: List<Entry<Int, String>>, currentWeekIndex: Int): Boolean {
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

    override fun getItemPosition(any: Any) = POSITION_NONE

    override fun getCount(): Int = fragmentsInfo.size

    override fun getPageTitle(position: Int): CharSequence = fragmentsInfo[position].value

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        state as Bundle?
        var s = state
        val isIndexValid = currentWeekIndex >= 0 && fragmentsInfo.size > currentWeekIndex
        if (isIndexValid && state?.getInt(ARG_WEEK_ID) != fragmentsInfo[currentWeekIndex].key) {
            s = null
        }
        super.restoreState(s, loader)
    }

    override fun saveState(): Parcelable? {
        var superState = super.saveState() as Bundle?
        val isIndexValid = currentWeekIndex >= 0 && fragmentsInfo.size > currentWeekIndex
        if (isIndexValid) {
            superState = superState ?: Bundle()
            superState.putInt(ARG_WEEK_ID, fragmentsInfo[currentWeekIndex].key)
        }
        return superState
    }
}
