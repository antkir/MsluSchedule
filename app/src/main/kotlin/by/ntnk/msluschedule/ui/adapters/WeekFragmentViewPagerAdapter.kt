package by.ntnk.msluschedule.ui.adapters

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import by.ntnk.msluschedule.db.data.Week
import by.ntnk.msluschedule.ui.week.WeekFragment
import by.ntnk.msluschedule.utils.DEFAULT

class WeekFragmentViewPagerAdapter(fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager) {

    private var weeks = listOf<Week>()

    var currentWeekPosition = Int.DEFAULT
        private set

    fun setData(weeks: List<Week>, currentWeekPosition: Int) {
        this.weeks = weeks
        this.currentWeekPosition = currentWeekPosition
    }

    fun onData(weeks: List<Week>, currentWeekPosition: Int): Boolean {
        if (this.weeks != weeks) {
            setData(weeks, currentWeekPosition)
            notifyDataSetChanged()
            return true
        }
        return false
    }

    override fun getFragment(position: Int): WeekFragment? {
        return super.getFragment(position) as WeekFragment?
    }

    override fun getItem(position: Int): Fragment {
        val weekId = weeks[position].id
        val isCurrentWeek = position == currentWeekPosition
        return WeekFragment.newInstance(weekId, isCurrentWeek)
    }

    override fun getItemPosition(any: Any): Int {
        return POSITION_NONE
    }

    override fun getCount(): Int {
        return weeks.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return weeks[position].value
    }

    override fun restoreState(state: Parcelable?, loader: ClassLoader?) {
        state as Bundle?
        val isValidIndex = currentWeekPosition in weeks.indices
        if (isValidIndex && state?.getInt(ARG_CURRENT_WEEK_ID) == weeks[currentWeekPosition].id) {
            super.restoreState(state, loader)
        }
    }

    override fun saveState(): Parcelable {
        val state = super.saveState() as? Bundle ?: Bundle()
        state.putInt(ARG_CURRENT_WEEK_ID, weeks[currentWeekPosition].id)
        return state
    }

    companion object {
        private const val ARG_CURRENT_WEEK_ID = "currentWeekId"
    }
}
