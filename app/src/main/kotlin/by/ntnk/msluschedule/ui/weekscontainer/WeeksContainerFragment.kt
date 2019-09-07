package by.ntnk.msluschedule.ui.weekscontainer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.viewpager.widget.ViewPager
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.mvp.views.MvpFragment
import by.ntnk.msluschedule.ui.adapters.WeekFragmentViewPagerAdapter
import by.ntnk.msluschedule.ui.warningdialog.WarningDialogFragment
import by.ntnk.msluschedule.utils.INVALID_VALUE
import by.ntnk.msluschedule.utils.ImmutableEntry
import com.google.android.material.tabs.TabLayout
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

private const val SELECTED_TAB_POSITION = "tab_position"
private const val WARNING_DIALOG_FRAGMENT = "WarningDialogFragment"

class WeeksContainerFragment :
        MvpFragment<WeeksContainerPresenter, WeeksContainerView>(),
        WeeksContainerView,
        WarningDialogFragment.DialogListener {
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private var savedCurrentPosition = INVALID_VALUE
    private var currentWeekItemIndex = INVALID_VALUE
    private lateinit var listener: OnScheduleContainerDeletedListener

    private val adapter: WeekFragmentViewPagerAdapter?
        get() = viewPager.adapter as WeekFragmentViewPagerAdapter?

    override val view: WeeksContainerView
        get() = this

    @Inject
    lateinit var injectedPresenter: Lazy<WeeksContainerPresenter>

    override fun onCreatePresenter(): WeeksContainerPresenter = injectedPresenter.get()

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        try {
            listener = context as OnScheduleContainerDeletedListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnScheduleContainerDeletedListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        savedCurrentPosition = savedInstanceState?.getInt(SELECTED_TAB_POSITION) ?: INVALID_VALUE
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_weekscontainer, container, false)
        viewPager = rootView.findViewById(R.id.viewpager_weekscontainer)
        tabLayout = rootView.findViewById(R.id.tabs_weekscontainer)
        tabLayout.setOnTouchListener { _, _ -> true }
        tabLayout.setupWithViewPager(viewPager)
        return rootView
    }

    override fun onStart() {
        super.onStart()
        presenter.initWeeksAdapter()
    }

    override fun onStop() {
        super.onStop()
        presenter.clearDisposables()
        savedCurrentPosition = tabLayout.selectedTabPosition
    }

    override fun initWeeksAdapter(weekIds: List<ImmutableEntry>, currentWeekItemIndex: Int) {
        this.currentWeekItemIndex = currentWeekItemIndex

        with(viewPager) {
            offscreenPageLimit = weekIds.size - 1
            if (adapter == null) {
                adapter = WeekFragmentViewPagerAdapter(childFragmentManager, weekIds, currentWeekItemIndex)
                if (savedCurrentPosition == INVALID_VALUE) {
                    savedCurrentPosition = currentWeekItemIndex
                }
            } else {
                val adapter = adapter as WeekFragmentViewPagerAdapter
                val haveDataChanged = adapter.swapData(weekIds, currentWeekItemIndex)
                if (haveDataChanged || savedCurrentPosition == INVALID_VALUE) {
                    savedCurrentPosition = currentWeekItemIndex
                }
            }
            setCurrentItem(savedCurrentPosition, false)
        }
    }

    fun swapTabs() {
        savedCurrentPosition = INVALID_VALUE
        presenter.initWeeksAdapter()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_TAB_POSITION, tabLayout.selectedTabPosition)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_weekscontainer_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.item_weekscontainer_today -> {
                viewPager.currentItem = currentWeekItemIndex
                adapter?.getFragment(currentWeekItemIndex)?.highlightToday()
                return true
            }
            R.id.item_weekscontainer_delete -> {
                val warningFragment = WarningDialogFragment()
                warningFragment.show(childFragmentManager, WARNING_DIALOG_FRAGMENT)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDeleteScheduleContainerClick() = presenter.deleteSelectedScheduleContainer()

    override fun removeScheduleContainerFromView(info: ScheduleContainerInfo) {
        listener.onScheduleContainerDeleted(info)
    }

    interface OnScheduleContainerDeletedListener {
        fun onScheduleContainerDeleted(info: ScheduleContainerInfo)
    }
}
