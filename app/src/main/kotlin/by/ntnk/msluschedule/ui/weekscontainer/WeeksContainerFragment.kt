package by.ntnk.msluschedule.ui.weekscontainer

import android.content.Context
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.ntnk.msluschedule.R
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

    override val view: WeeksContainerView
        get() = this

    @Inject
    lateinit var injectedPresenter: WeeksContainerPresenter

    override fun onCreatePresenter(): WeeksContainerPresenter = injectedPresenter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
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
}
