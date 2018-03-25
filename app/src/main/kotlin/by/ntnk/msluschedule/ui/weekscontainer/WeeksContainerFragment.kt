package by.ntnk.msluschedule.ui.weekscontainer

import android.content.Context
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.view.*
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.mvp.views.MvpFragment
import by.ntnk.msluschedule.ui.adapters.ViewPagerFragmentAdapter
import by.ntnk.msluschedule.utils.ImmutableEntry
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

private const val SELECTED_TAB_POSITION = "tab_position"

class WeeksContainerFragment :
        MvpFragment<WeeksContainerPresenter, WeeksContainerView>(),
        WeeksContainerView {
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private lateinit var fragmentAdapter: ViewPagerFragmentAdapter
    private var savedCurrentPosition = -1
    private var listener: OnScheduleContainerRemovedListener? = null

    override val view: WeeksContainerView
        get() = this

    @Inject
    lateinit var injectedPresenter: WeeksContainerPresenter

    override fun onCreatePresenter(): WeeksContainerPresenter = injectedPresenter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        try {
            listener = context as OnScheduleContainerRemovedListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() +
                    " must implement OnScheduleContainerRemovedListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_weekscontainer, container, false)
        viewPager = view.findViewById(R.id.viewpager_weekscontainer)
        tabLayout = view.findViewById(R.id.tabs_weekscontainer)
        viewPager.offscreenPageLimit = 2
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        savedCurrentPosition = savedInstanceState?.getInt(SELECTED_TAB_POSITION) ?: -1
        presenter.initWeeksAdapter()
    }

    override fun initWeeksAdapter(weekIds: List<ImmutableEntry>, currentWeekItemIndex: Int) {
        fragmentAdapter = ViewPagerFragmentAdapter(childFragmentManager, weekIds)
        viewPager.adapter = fragmentAdapter
        viewPager.currentItem = currentWeekItemIndex
        viewPager.currentItem =
                if (savedCurrentPosition == -1) currentWeekItemIndex else savedCurrentPosition
        tabLayout.setupWithViewPager(viewPager)
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
            R.id.item_weekscontainer_remove -> {
                presenter.removeSelectedScheduleContainer()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun removeScheduleContainerFromView(info: ScheduleContainerInfo) {
        listener!!.onScheduleContainerRemoved(info)
    }

    interface OnScheduleContainerRemovedListener {
        fun onScheduleContainerRemoved(info: ScheduleContainerInfo)
    }
}
