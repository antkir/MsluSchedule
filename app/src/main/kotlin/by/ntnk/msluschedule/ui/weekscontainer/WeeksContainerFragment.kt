package by.ntnk.msluschedule.ui.weekscontainer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.view.*
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.mvp.views.MvpFragment
import by.ntnk.msluschedule.ui.adapters.WeekFragmentViewPagerAdapter
import by.ntnk.msluschedule.ui.warningdialog.WarningDialogFragment
import by.ntnk.msluschedule.utils.INVALID_VALUE
import by.ntnk.msluschedule.utils.ImmutableEntry
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_weekscontainer.*
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

    private lateinit var listener: OnScheduleContainerDeletedListener

    override val view: WeeksContainerView
        get() = this

    @Inject
    lateinit var injectedPresenter: Lazy<WeeksContainerPresenter>

    override fun onCreatePresenter(): WeeksContainerPresenter = injectedPresenter.get()

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        try {
            listener = context as OnScheduleContainerDeletedListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement OnScheduleContainerDeletedListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_weekscontainer, container, false)
        viewPager = view.findViewById(R.id.viewpager_weekscontainer)
        tabLayout = view.findViewById(R.id.tabs_weekscontainer)
        tabLayout.setOnTouchListener { _, _ -> true }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savedCurrentPosition = savedInstanceState?.getInt(SELECTED_TAB_POSITION) ?: INVALID_VALUE
        presenter.initWeeksAdapter()
    }

    override fun initWeeksAdapter(weekIds: List<ImmutableEntry>, currentWeekItemIndex: Int) {
        viewpager_weekscontainer.visibility = View.VISIBLE
        val viewPagerAdapter = WeekFragmentViewPagerAdapter(childFragmentManager, weekIds, currentWeekItemIndex)
        with(viewPager) {
            adapter = viewPagerAdapter
            offscreenPageLimit = weekIds.size - 1
            currentItem = if (savedCurrentPosition == INVALID_VALUE) currentWeekItemIndex else savedCurrentPosition
        }
        tabLayout.setupWithViewPager(viewPager)
    }

    fun swapTabs() {
        savedCurrentPosition = INVALID_VALUE
        presenter.initWeeksAdapter()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(SELECTED_TAB_POSITION, tabLayout.selectedTabPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.fragment_weekscontainer_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        super.onOptionsItemSelected(item)
        return when (item?.itemId) {
            R.id.item_weekscontainer_today -> {
                val index = presenter.getCurrentWeekIndex()
                        .coerceAtLeast(0)
                        .coerceAtMost((viewPager.adapter?.count ?: 1) - 1)
                viewPager.currentItem = index
                val adapter = viewPager.adapter as WeekFragmentViewPagerAdapter?
                adapter?.getWeekFragment(index)?.showToday()
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
