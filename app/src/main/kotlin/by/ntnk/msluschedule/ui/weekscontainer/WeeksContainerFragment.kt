package by.ntnk.msluschedule.ui.weekscontainer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.text.TextUtilsCompat
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.databinding.FragmentWeekscontainerBinding
import by.ntnk.msluschedule.mvp.views.MvpFragment
import by.ntnk.msluschedule.ui.adapters.WeekFragmentViewPagerAdapter
import by.ntnk.msluschedule.ui.deleteschedule.DeleteScheduleFragment
import by.ntnk.msluschedule.utils.INVALID_VALUE
import by.ntnk.msluschedule.utils.ImmutableEntry
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import java.util.Locale
import javax.inject.Inject

private const val SELECTED_TAB_POSITION = "tab_position"
private const val DELETE_SCHEDULE_FRAGMENT = "DeleteScheduleFragment"

class WeeksContainerFragment :
    MvpFragment<WeeksContainerPresenter, WeeksContainerView>(),
    WeeksContainerView,
    DeleteScheduleFragment.DialogListener {

    private var savedCurrentPosition = INVALID_VALUE
    private var currentWeekItemIndex = INVALID_VALUE
    private lateinit var listener: OnScheduleContainerDeletedListener

    private var fragmentBinding: FragmentWeekscontainerBinding? = null
    private val binding get() = fragmentBinding!!

    private val adapter: WeekFragmentViewPagerAdapter?
        get() = binding.viewpagerWeeks.adapter as WeekFragmentViewPagerAdapter?

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
        savedCurrentPosition = savedInstanceState?.getInt(SELECTED_TAB_POSITION) ?: INVALID_VALUE
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentBinding = FragmentWeekscontainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tabLayoutWeeks.setOnTouchListener { _, _ -> true }
        binding.tabLayoutWeeks.setupWithViewPager(binding.viewpagerWeeks)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            binding.tabLayoutWeeks.layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.fragment_weekscontainer_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.item_weekscontainer_today -> {
                            binding.viewpagerWeeks.currentItem = currentWeekItemIndex
                            adapter?.getFragment(currentWeekItemIndex)?.highlightToday()
                            return true
                        }
                        R.id.item_weekscontainer_delete -> {
                            val deleteScheduleFragment = DeleteScheduleFragment()
                            deleteScheduleFragment.show(childFragmentManager, DELETE_SCHEDULE_FRAGMENT)
                            return true
                        }
                    }
                    return false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun onStart() {
        super.onStart()
        val isRTL = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL
        presenter.initWeeksAdapter(isRTL)
    }

    override fun onStop() {
        super.onStop()
        presenter.clearDisposables()
        savedCurrentPosition = binding.tabLayoutWeeks.selectedTabPosition
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
    }

    override fun initWeeksAdapter(weekIds: List<ImmutableEntry>, currentWeekItemIndex: Int) {
        this.currentWeekItemIndex = currentWeekItemIndex

        with(binding.viewpagerWeeks) {
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
        val isRTL = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL
        presenter.initWeeksAdapter(isRTL)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_TAB_POSITION, binding.tabLayoutWeeks.selectedTabPosition)
    }

    override fun onDeleteScheduleContainerClick() = presenter.deleteSelectedScheduleContainer()

    override fun removeScheduleContainerFromView(info: ScheduleContainerInfo) {
        listener.onScheduleContainerDeleted(info)
    }

    interface OnScheduleContainerDeletedListener {
        fun onScheduleContainerDeleted(info: ScheduleContainerInfo)
    }
}
