package by.ntnk.msluschedule.ui.weekscontainer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.ScheduleContainer
import by.ntnk.msluschedule.data.Week
import by.ntnk.msluschedule.databinding.FragmentWeekscontainerBinding
import by.ntnk.msluschedule.mvp.views.MvpFragment
import by.ntnk.msluschedule.ui.adapters.WeekFragmentViewPagerAdapter
import by.ntnk.msluschedule.ui.deleteschedule.DeleteScheduleFragment
import by.ntnk.msluschedule.ui.weekscontainer.WeeksContainerPresenter.Companion.CENTRAL_VIEW_POSITION
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.ErrorType
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import javax.inject.Inject

class WeeksContainerFragment :
    MvpFragment<WeeksContainerPresenter, WeeksContainerView>(),
    WeeksContainerView,
    DeleteScheduleFragment.DialogListener {

    private var fragmentBinding: FragmentWeekscontainerBinding? = null
    private val binding get() = fragmentBinding!!
    private lateinit var listener: OnScheduleContainerDeletedListener
    private var restoredAdapterPosition = Int.DEFAULT
    private val disposables: CompositeDisposable = CompositeDisposable()

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentBinding = FragmentWeekscontainerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupTabLayout()

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.fragment_weekscontainer_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.item_weekscontainer_today -> {
                            val adapter = binding.viewpagerWeeks.adapter as WeekFragmentViewPagerAdapter
                            binding.viewpagerWeeks.currentItem = adapter.currentWeekPosition
                            adapter.getFragment(adapter.currentWeekPosition)?.highlightToday()
                            return true
                        }

                        R.id.item_weekscontainer_delete -> {
                            val deleteScheduleFragment = DeleteScheduleFragment()
                            deleteScheduleFragment.show(childFragmentManager, DeleteScheduleFragment.FRAGMENT_TAG)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTabLayout() {
        binding.tabLayoutWeeks.setupWithViewPager(binding.viewpagerWeeks)
        binding.tabLayoutWeeks.setOnTouchListener { _, _ -> true }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        restoredAdapterPosition = savedInstanceState?.getInt(SELECTED_TAB_POSITION) ?: Int.DEFAULT
    }

    override fun onStart() {
        super.onStart()

        presenter.updateCurrentAcademicWeek()

        disposables += presenter.uiStateObservable
            .map { it.dataState }
            .distinctUntilChanged()
            .subscribe { dataState ->
                if (dataState.data != null && dataState.currentAcademicWeekIndex != Int.DEFAULT) {
                    onData(dataState.data, dataState.currentAcademicWeekIndex)
                }
            }

        disposables += presenter.uiStateObservable
            .map { it.academicWeekChangedEvent }
            .distinctUntilChanged()
            .subscribe { academicWeekChangedEvent ->
                if (academicWeekChangedEvent.isTriggered) {
                    val scheduleContainer = presenter.getSelectedScheduleContainer()
                    presenter.getData(scheduleContainer)
                    presenter.academicWeekChangedEventConsumed()
                }
            }

        disposables += presenter.uiStateObservable
            .map { it.scheduleContainerDeletedEvent }
            .distinctUntilChanged()
            .subscribe { scheduleContainerDeletedEvent ->
                if (scheduleContainerDeletedEvent.isTriggered) {
                    listener.onScheduleContainerDeleted(scheduleContainerDeletedEvent.data)
                    presenter.scheduleContainerDeletedEventConsumed()
                }
            }

        disposables += presenter.uiStateObservable
            .map { it.errorEvent }
            .distinctUntilChanged()
            .subscribe { errorEvent ->
                if (errorEvent.error != null) {
                    showError(errorEvent.error)
                    presenter.errorEventConsumed()
                }
            }
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
    }

    private fun onData(weeks: List<Week>, currentAcademicWeekIndex: Int) {
        val currentWeekPosition = minOf(currentAcademicWeekIndex, CENTRAL_VIEW_POSITION)

        with(binding.viewpagerWeeks) {
            offscreenPageLimit = weeks.size - 1

            var isNewData = false
            // Initialize adapter here because otherwise TabLayout does not properly scroll to the
            // restored adapter position after calling setCurrentItem function.
            if (adapter == null) {
                isNewData = true
                val weekAdapter = WeekFragmentViewPagerAdapter(childFragmentManager)
                // Set initial data here so adapter's state is restored properly when setAdapter is called in ViewPager.
                weekAdapter.setData(weeks, currentWeekPosition)
                adapter = weekAdapter
            }

            val adapter = adapter as WeekFragmentViewPagerAdapter
            isNewData = adapter.onData(weeks, currentWeekPosition) || isNewData

            if (restoredAdapterPosition != Int.DEFAULT) {
                setCurrentItem(restoredAdapterPosition, false)
                restoredAdapterPosition = Int.DEFAULT
            } else if (isNewData) {
                setCurrentItem(currentWeekPosition, false)
            }
        }
    }

    fun swapData() {
        val scheduleContainer = presenter.getSelectedScheduleContainer()
        presenter.getData(scheduleContainer)
    }

    private fun showError(error: ErrorType) {
        val errorMessageResource = AndroidUtils.getErrorMessageResId(error)
        val snackbar = Snackbar.make(binding.root, errorMessageResource, Snackbar.LENGTH_LONG)
        snackbar.anchorView = requireActivity().findViewById(R.id.fab_base)
        snackbar.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SELECTED_TAB_POSITION, binding.tabLayoutWeeks.selectedTabPosition)
    }

    override fun onDeleteScheduleContainerClick() {
        presenter.deleteSelectedScheduleContainer()
    }

    interface OnScheduleContainerDeletedListener {
        fun onScheduleContainerDeleted(scheduleContainer: ScheduleContainer)
    }

    companion object {
        private const val SELECTED_TAB_POSITION = "selectedTabPosition"
    }
}
