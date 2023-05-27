package by.ntnk.msluschedule.ui.week

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.ScheduleContainerInfo
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.databinding.FragmentWeekBinding
import by.ntnk.msluschedule.mvp.views.MvpFragment
import by.ntnk.msluschedule.ui.adapters.LessonRecyclerViewAdapter
import by.ntnk.msluschedule.ui.lessoninfo.LessonInfoActivity
import by.ntnk.msluschedule.ui.weekday.WeekdayActivity
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.Utils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WeekFragment : MvpFragment<WeekPresenter, WeekView>(), WeekView {

    private var isNoClassesViewVisible: Boolean = true
    private var weekId: Int = Int.DEFAULT
    private var isCurrentWeek: Boolean = false
    private var restoredRecyclerViewState: Parcelable? = null
    private lateinit var adapterOnClickDisposable: Disposable
    private val disposables: CompositeDisposable = CompositeDisposable()
    private lateinit var scheduleContainer: ScheduleContainerInfo

    private var fragmentBinding: FragmentWeekBinding? = null
    private val binding get() = fragmentBinding!!

    override val view: WeekView
        get() = this

    @Inject
    lateinit var injectedPresenter: Lazy<WeekPresenter>

    override fun onCreatePresenter(): WeekPresenter = injectedPresenter.get()

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = arguments?.getInt(ARG_WEEK_ID)
        weekId = id!!
        isCurrentWeek = arguments?.getBoolean(ARG_IS_CURRENT_WEEK) == true
        scheduleContainer = presenter.getScheduleContainer()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        fragmentBinding = FragmentWeekBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerView()

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false

            if (AndroidUtils.isNetworkAccessible(requireContext().applicationContext)) {
                presenter.updateSchedule(scheduleContainer.id, weekId)
            } else {
                val baseFABMain = requireActivity().findViewById<FloatingActionButton>(R.id.fab_base)
                AndroidUtils.showSnackbarNetworkInaccessible(baseFABMain)
            }
        }

        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.fragment_week_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.item_week_update -> {
                            if (AndroidUtils.isNetworkAccessible(requireContext().applicationContext)) {
                                presenter.updateSchedule(scheduleContainer.id, weekId)
                            } else {
                                val baseFABMain = requireActivity().findViewById<FloatingActionButton>(R.id.fab_base)
                                AndroidUtils.showSnackbarNetworkInaccessible(baseFABMain)
                            }
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

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        restoredRecyclerViewState = savedInstanceState?.getParcelable(ARG_RECYCLER_VIEW_SAVED_STATE)
    }

    private fun setupRecyclerView() {
        binding.recyclerviewDays.apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = LessonRecyclerViewAdapter()
            observeAdapterOnClick(adapter as LessonRecyclerViewAdapter)
        }
    }

    private fun observeAdapterOnClick(adapter: LessonRecyclerViewAdapter) {
        adapterOnClickDisposable = adapter.onClickObservable.subscribeBy(
            onNext = { itemView ->
                when (itemView.viewType) {
                    LessonRecyclerViewAdapter.ItemViewType.DAY -> {
                        val weekdayId = (itemView as LessonRecyclerViewAdapter.DayItemView).weekdayId
                        WeekdayActivity.startActivity(requireContext(), weekdayId)
                    }

                    LessonRecyclerViewAdapter.ItemViewType.STUDYGROUPCLASS -> {
                        val classId = (itemView as LessonRecyclerViewAdapter.StudyGroupClassItemView).classDesciptor.id
                        LessonInfoActivity.startActivity(requireContext(), classId, ScheduleType.STUDYGROUP, weekId)
                    }

                    LessonRecyclerViewAdapter.ItemViewType.TEACHERCLASS -> {
                        val classId = (itemView as LessonRecyclerViewAdapter.TeacherClassItemView).classDesciptor.id
                        LessonInfoActivity.startActivity(requireContext(), classId, ScheduleType.TEACHER, weekId)
                    }

                    else -> {}
                }
            },
            onError = { throwable -> Timber.e(throwable) }
        )
    }

    override fun onStart() {
        super.onStart()

        disposables += presenter.scheduleUiStateObservable
            .subscribeBy(
                onNext = { scheduleUiState ->
                    if (scheduleUiState == ScheduleUiState()) {
                        presenter.checkWeekInitialized(weekId)
                    }

                    if (scheduleUiState.isInitializingWeek ||
                        scheduleUiState.isWeekInitializedNoData ||
                        scheduleUiState.isProcessingFirstUpdate ||
                        scheduleUiState.isFirstUpdateError
                    ) {
                        binding.progressIndicatorInit.visibility = View.VISIBLE
                    } else {
                        binding.progressIndicatorInit.visibility = View.GONE
                    }

                    if (scheduleUiState.isWeekInitializedNoData) {
                        presenter.updateSchedule(scheduleContainer.id, weekId, isFirstUpdate = true)
                    }

                    if (scheduleUiState.isWeekInitialized) {
                        presenter.getSchedule(scheduleContainer.type, weekId)
                    }

                    if (scheduleUiState.isData()) {
                        onScheduleData(scheduleUiState.data)
                        for (weekday in scheduleUiState.data) {
                            setNotesStatus(weekday.weekdayId)
                        }
                    }

                    if (scheduleUiState.isError()) {
                        val throwable = scheduleUiState.throwable!!
                        if (scheduleUiState.isFirstUpdateError) {
                            presenter.getSchedule(scheduleContainer.type, weekId)
                            if (Utils.isNetworkException(throwable)) {
                                Timber.i(throwable)
                                showError(throwable, isNetworkError = true)
                            } else {
                                Timber.e(throwable)
                                showError(throwable, isNetworkError = false)
                            }
                        } else {
                            Timber.e(throwable)
                            showError(throwable, isNetworkError = false)
                        }
                    }
                }
            )

        disposables += presenter.scheduleUpdateEventObservable
            .subscribeBy(
                onNext = { scheduleUpdateEvent ->
                    if (scheduleUpdateEvent.isLoading) {
                        showUpdateProgressIndicator()
                    } else {
                        hideUpdateProgressIndicator()
                    }

                    if (scheduleUpdateEvent.isEvent()) {
                        scheduleUpdateEvent.consumeEvent()
                        if (!checkUiEventExpired(scheduleUpdateEvent.timestamp)) {
                            showSuccessfulUpdateToast()
                        }
                    }

                    if (scheduleUpdateEvent.isError()) {
                        val throwable = scheduleUpdateEvent.consumeError()
                        if (Utils.isNetworkException(throwable)) {
                            Timber.i(throwable)
                            if (!checkUiEventExpired(scheduleUpdateEvent.timestamp)) {
                                showError(throwable, isNetworkError = true)
                            }
                        } else {
                            Timber.e(throwable)
                            if (!checkUiEventExpired(scheduleUpdateEvent.timestamp)) {
                                showError(throwable, isNetworkError = false)
                            }
                        }
                    }
                }
            )
    }

    private fun setNotesStatus(weekdayId: Int) {
        disposables += presenter.hasNotes(weekdayId)
            .subscribeBy(
                onSuccess = { hasNotes ->
                    val adapter = binding.recyclerviewDays.adapter as LessonRecyclerViewAdapter
                    adapter.updateWeekdayNotesStatus(weekdayId, hasNotes)
                },
                onError = { throwable -> Timber.e(throwable) }
            )
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = binding.recyclerviewDays.layoutManager?.onSaveInstanceState()
        outState.putParcelable(ARG_RECYCLER_VIEW_SAVED_STATE, state)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapterOnClickDisposable.dispose()
        binding.progressIndicatorUpdate.animate().setListener(null)
        binding.swipeRefreshLayout.setOnRefreshListener(null)
        binding.recyclerviewDays.clearOnScrollListeners()
        fragmentBinding = null
    }

    fun highlightToday() {
        if (binding.textNoClasses.visibility != View.VISIBLE) {
            val adapter = binding.recyclerviewDays.adapter as LessonRecyclerViewAdapter
            val index = adapter.getWeekdayViewIndex(presenter.getCurrentDayOfWeek())
            val smoothScroller = object : LinearSmoothScroller(requireContext()) {
                override fun getVerticalSnapPreference() = SNAP_TO_START
            }
            smoothScroller.targetPosition = index
            binding.recyclerviewDays.layoutManager?.startSmoothScroll(smoothScroller)
            val fromColor = ContextCompat.getColor(requireContext(), R.color.surface)
            val toColor = ContextCompat.getColor(requireContext(), R.color.item_highlight)
            val highlightAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
            highlightAnimation.duration = 500
            highlightAnimation.repeatMode = ValueAnimator.REVERSE
            highlightAnimation.repeatCount = 1
            highlightAnimation.addUpdateListener { animator ->
                val view: View? = binding.recyclerviewDays.layoutManager?.findViewByPosition(index)
                view?.setBackgroundColor(animator.animatedValue as Int)
            }
            highlightAnimation.start()
        }
    }

    private fun onScheduleData(data: List<WeekdayWithLessons<Lesson>>) {
        if (data.sumOf { weekday -> weekday.classes.size } == 0) {
            binding.buttonSwitchNoClassesLayout.visibility = View.VISIBLE
            if (isNoClassesViewVisible) {
                binding.textNoClasses.visibility = View.VISIBLE
            } else {
                binding.textNoClasses.visibility = View.GONE
            }
            binding.recyclerviewDays.clearOnScrollListeners()
            binding.recyclerviewDays.addOnScrollListener(recyclerViewScrollListener)
            binding.buttonSwitchNoClassesLayout.setOnClickListener { onSwitchNoClassesLayoutButtonClickListener() }
        } else {
            binding.buttonSwitchNoClassesLayout.visibility = View.GONE
            binding.textNoClasses.visibility = View.GONE
            binding.recyclerviewDays.clearOnScrollListeners()
        }

        val adapter = binding.recyclerviewDays.adapter as LessonRecyclerViewAdapter
        val hadNoClasses = !adapter.hasClasses()

        adapter.onNewData(data)

        val layoutManager = binding.recyclerviewDays.layoutManager as LinearLayoutManager
        if (restoredRecyclerViewState != null) {
            layoutManager.onRestoreInstanceState(restoredRecyclerViewState)
            restoredRecyclerViewState = null
        } else {
            if (isCurrentWeek && hadNoClasses && adapter.hasClasses()) {
                val index = adapter.getWeekdayViewIndex(presenter.getCurrentDayOfWeek())
                layoutManager.scrollToPositionWithOffset(index, 0)
            }
        }

        if (isCurrentWeek) {
            activity?.findViewById<CircularProgressIndicator>(R.id.progressbar)?.visibility = View.GONE
            parentFragment?.view?.findViewById<ViewPager>(R.id.viewpager_weeks)?.visibility = View.VISIBLE
        }
    }

    private val recyclerViewScrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (binding.buttonSwitchNoClassesLayout.visibility == View.VISIBLE && dy > 0) {
                    binding.buttonSwitchNoClassesLayout.visibility = View.GONE
                    val anim = AnimationUtils.loadAnimation(
                        binding.buttonSwitchNoClassesLayout.context,
                        R.anim.button_week_slide_down
                    )
                    binding.buttonSwitchNoClassesLayout.startAnimation(anim)
                } else if (binding.buttonSwitchNoClassesLayout.visibility != View.VISIBLE && dy < 0) {
                    binding.buttonSwitchNoClassesLayout.visibility = View.VISIBLE
                    val anim = AnimationUtils.loadAnimation(
                        binding.buttonSwitchNoClassesLayout.context,
                        R.anim.button_week_slide_up
                    )
                    binding.buttonSwitchNoClassesLayout.startAnimation(anim)
                }
            }
        }

    private fun onSwitchNoClassesLayoutButtonClickListener() {
        if (isNoClassesViewVisible) {
            isNoClassesViewVisible = false

            binding.textNoClasses.visibility = View.GONE
            binding.buttonSwitchNoClassesLayout.text = requireContext().getString(R.string.button_week_hide_weekdays)
        } else {
            isNoClassesViewVisible = true

            binding.textNoClasses.visibility = View.VISIBLE
            binding.buttonSwitchNoClassesLayout.text = requireContext().getString(R.string.button_week_show_weekdays)

            // workaround the edge case when the button is pressed during
            // the hiding animation and won't show again
            binding.recyclerviewDays.suppressLayout(true)
            binding.recyclerviewDays.suppressLayout(false)
            binding.recyclerviewDays.scrollBy(0, -1)
        }
    }

    private fun hideUpdateProgressIndicator() {
        binding.recyclerviewDays.animate()
            .translationY(0f)
            .setDuration(200)
            .start()
        binding.progressIndicatorUpdate.animate()
            .translationY(-binding.progressIndicatorUpdate.height.toFloat())
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.progressIndicatorUpdate.animate()?.setListener(null)
                    binding.progressIndicatorUpdate.visibility = View.GONE
                }
            })
            .start()
    }

    private fun showUpdateProgressIndicator() {
        binding.progressIndicatorUpdate.visibility = View.VISIBLE
        binding.recyclerviewDays.animate()
            .translationY(binding.progressIndicatorUpdate.height.toFloat())
            .setDuration(200)
            .start()
        binding.progressIndicatorUpdate.y = -binding.progressIndicatorUpdate.height.toFloat()
        binding.progressIndicatorUpdate.animate()
            .translationY(0f)
            .setDuration(200)
            .start()
    }

    private fun showSuccessfulUpdateToast() {
        Handler(Looper.getMainLooper()).postDelayed(ScheduleUpdateNotificationRunnable(context), 250)
    }

    private class ScheduleUpdateNotificationRunnable(context: Context?) : Runnable {
        private val contextWeakRef: WeakReference<Context?> = WeakReference(context)

        override fun run() {
            val context = contextWeakRef.get()
            if (context != null) {
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.messsage_schedule_update_successful),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showError(t: Throwable, isNetworkError: Boolean) {
        val baseFABMain = requireActivity().findViewById<FloatingActionButton>(R.id.fab_base)
        val snackbar = Snackbar.make(requireView(), AndroidUtils.getErrorMessageResId(t), 5000)
            .setAnchorView(baseFABMain)
        if (isNetworkError) {
            snackbar.setAction(R.string.snackbar_week_init_retry) {
                context ?: return@setAction
                if (AndroidUtils.isNetworkAccessible(requireContext().applicationContext)) {
                    presenter.updateSchedule(scheduleContainer.id, weekId)
                } else {
                    AndroidUtils.showSnackbarNetworkInaccessible(baseFABMain)
                }
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
        snackbar.show()
    }

    /**
     * It would be confusing for users to see successful update or error notifications when
     * a schedule update UI event wasn't consumed and user maximizes the app after some time.
     * So limit event's lifetime and use this function to check if the unconsumed event is expired.
     */
    private fun checkUiEventExpired(eventTimestamp: Long): Boolean {
        val timeDelta = System.nanoTime() - eventTimestamp
        return timeDelta > TimeUnit.MINUTES.toNanos(2)
    }

    companion object {
        private const val ARG_WEEK_ID = "weekID"
        private const val ARG_IS_CURRENT_WEEK = "isCurrentWeek"
        private const val ARG_RECYCLER_VIEW_SAVED_STATE = "recyclerViewSavedState"

        fun newInstance(weekId: Int, isCurrentWeek: Boolean): WeekFragment {
            return WeekFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_WEEK_ID, weekId)
                    putBoolean(ARG_IS_CURRENT_WEEK, isCurrentWeek)
                }
            }
        }
    }
}
