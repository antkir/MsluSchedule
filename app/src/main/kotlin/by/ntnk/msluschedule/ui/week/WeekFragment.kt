package by.ntnk.msluschedule.ui.week

import android.animation.Animator
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
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.databinding.FragmentWeekBinding
import by.ntnk.msluschedule.mvp.views.MvpFragment
import by.ntnk.msluschedule.ui.adapters.LessonRecyclerViewAdapter
import by.ntnk.msluschedule.ui.adapters.VIEWTYPE_STUDYGROUP
import by.ntnk.msluschedule.ui.adapters.VIEWTYPE_TEACHER
import by.ntnk.msluschedule.ui.adapters.VIEWTYPE_WEEKDAY
import by.ntnk.msluschedule.ui.lessoninfo.LessonInfoActivity
import by.ntnk.msluschedule.ui.weekday.WeekdayActivity
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.INVALID_VALUE
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.SimpleAnimatorListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

private const val ARG_LAYOUT_MANAGER_SAVED_STATE = "argLayoutManagerSavedState"

class WeekFragment : MvpFragment<WeekPresenter, WeekView>(), WeekView {

    private lateinit var smoothScroller: RecyclerView.SmoothScroller
    private var isEmptyScheduleDaysVisible: Boolean = false
    private var weekId: Int = INVALID_VALUE
    private var isCurrentWeek: Boolean = false
    private var layoutManagerSavedState: Parcelable? = null
    private lateinit var adapterOnClickDisposable: Disposable

    private lateinit var binding: FragmentWeekBinding

    private val adapter: LessonRecyclerViewAdapter
        get() = binding.recyclerViewDays.adapter as LessonRecyclerViewAdapter? ?: LessonRecyclerViewAdapter()

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
        weekId = arguments?.getInt(ARG_WEEK_ID) ?: INVALID_VALUE
        isCurrentWeek = arguments?.getBoolean(ARG_IS_CURRENT_WEEK) == true
        layoutManagerSavedState = savedInstanceState?.getParcelable(ARG_LAYOUT_MANAGER_SAVED_STATE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentWeekBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initRecyclerView()

        binding.swipeRefreshLayout.setOnRefreshListener {
            binding.swipeRefreshLayout.isRefreshing = false
            if (AndroidUtils.isNetworkAccessible(requireContext().applicationContext)) {
                if (!isEmptyScheduleDaysVisible) {
                    layoutManagerSavedState = binding.recyclerViewDays.layoutManager?.onSaveInstanceState()
                }
                presenter.updateSchedule(weekId)
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
                                if (weekId != INVALID_VALUE) {
                                    layoutManagerSavedState = binding.recyclerViewDays.layoutManager?.onSaveInstanceState()
                                    presenter.updateSchedule(weekId)
                                }
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

    private fun initRecyclerView() {
        binding.recyclerViewDays.apply {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = this@WeekFragment.adapter
            initAdapterOnClickListener()
        }
        smoothScroller = object : LinearSmoothScroller(requireContext()) {
            override fun getVerticalSnapPreference() = SNAP_TO_START
        }
    }

    private fun initAdapterOnClickListener() {
        adapterOnClickDisposable = adapter.onClickObservable.subscribeBy(
            onNext = {
                when (it.viewType) {
                    VIEWTYPE_WEEKDAY -> {
                        val weekdayId = (it as LessonRecyclerViewAdapter.DayView).weekdayId
                        WeekdayActivity.startActivity(requireContext(), weekdayId)
                    }
                    VIEWTYPE_STUDYGROUP -> {
                        val lessonId = (it as LessonRecyclerViewAdapter.StudyGroupLessonView).lesson.id
                        LessonInfoActivity.startActivity(requireContext(), lessonId, ScheduleType.STUDYGROUP, weekId)
                    }
                    VIEWTYPE_TEACHER -> {
                        val lessonId = (it as LessonRecyclerViewAdapter.TeacherLessonView).lesson.id
                        LessonInfoActivity.startActivity(requireContext(), lessonId, ScheduleType.TEACHER, weekId)
                    }
                }
            },
            onError = { throwable -> Timber.e(throwable) }
        )
    }

    override fun onStart() {
        super.onStart()
        val shouldUpdateAdapter = adapter.itemCount == 0
        presenter.getSchedule(weekId, shouldUpdateAdapter)
    }

    override fun onStop() {
        presenter.clearDisposables()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        layoutManagerSavedState = binding.recyclerViewDays.layoutManager?.onSaveInstanceState()
        outState.putParcelable(ARG_LAYOUT_MANAGER_SAVED_STATE, layoutManagerSavedState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapterOnClickDisposable.dispose()
    }

    fun highlightToday() {
        if (binding.textNoLessons.visibility != View.VISIBLE) {
            val index = adapter.getWeekdayViewIndex(presenter.getCurrentDayOfWeek())
            smoothScroller.targetPosition = index
            binding.recyclerViewDays.layoutManager?.startSmoothScroll(smoothScroller)
            val fromColor = ContextCompat.getColor(requireContext(), R.color.surface)
            val toColor = ContextCompat.getColor(requireContext(), R.color.item_highlight)
            val highlightAnimation = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
            highlightAnimation.duration = 500
            highlightAnimation.addUpdateListener { animator ->
                val view: View? = binding.recyclerViewDays.layoutManager?.findViewByPosition(index)
                view?.setBackgroundColor(animator.animatedValue as Int)
            }
            highlightAnimation.repeatMode = ValueAnimator.REVERSE
            highlightAnimation.repeatCount = 1
            highlightAnimation.start()
        }
    }

    override fun showSchedule(data: List<WeekdayWithLessons<Lesson>>) {
        if (data.sumOf { it.lessons.size } == 0) {
            binding.buttonDaysVisibility.visibility = View.VISIBLE
            if (!isEmptyScheduleDaysVisible) {
                binding.textNoLessons.visibility = View.VISIBLE
            } else {
                binding.textNoLessons.visibility = View.GONE
            }
            binding.recyclerViewDays.addOnScrollListener(recyclerViewScrollListener)
            binding.buttonDaysVisibility.setOnClickListener { onWeekdaysButtonClickListener() }
        } else {
            binding.buttonDaysVisibility.visibility = View.GONE
            binding.textNoLessons.visibility = View.GONE
            binding.recyclerViewDays.removeOnScrollListener(recyclerViewScrollListener)
        }

        adapter.initData(data)

        val layoutManager = binding.recyclerViewDays.layoutManager as LinearLayoutManager
        layoutManager.onRestoreInstanceState(layoutManagerSavedState)
        if (isCurrentWeek) {
            if (layoutManagerSavedState == null) {
                val index = adapter.getWeekdayViewIndex(presenter.getCurrentDayOfWeek())
                layoutManager.scrollToPosition(index)
            }
            activity?.findViewById<CircularProgressIndicator>(R.id.progressbar)?.visibility = View.GONE
            parentFragment?.view?.findViewById<ViewPager>(R.id.viewpager_weeks)?.visibility = View.VISIBLE
        }
    }

    private val recyclerViewScrollListener: RecyclerView.OnScrollListener =
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (binding.buttonDaysVisibility.visibility == View.VISIBLE && dy > 0) {
                    binding.buttonDaysVisibility.visibility = View.GONE
                    val anim = AnimationUtils.loadAnimation(
                        binding.buttonDaysVisibility.context,
                        R.anim.button_week_slide_down
                    )
                    binding.buttonDaysVisibility.startAnimation(anim)
                } else if (binding.buttonDaysVisibility.visibility != View.VISIBLE && dy < 0) {
                    binding.buttonDaysVisibility.visibility = View.VISIBLE
                    val anim = AnimationUtils.loadAnimation(
                        binding.buttonDaysVisibility.context,
                        R.anim.button_week_slide_up
                    )
                    binding.buttonDaysVisibility.startAnimation(anim)
                }
            }
        }

    private fun onWeekdaysButtonClickListener() {
        if (!isEmptyScheduleDaysVisible) {
            isEmptyScheduleDaysVisible = true
            binding.textNoLessons.visibility = View.GONE

            binding.buttonDaysVisibility.text = requireContext().getString(R.string.button_week_hide_weekdays)
        } else {
            isEmptyScheduleDaysVisible = false
            binding.textNoLessons.visibility = View.VISIBLE

            // workaround the edge case, when the button is pressed during
            // the hiding animation and won't show again
            binding.recyclerViewDays.smoothScrollBy(0, -1)

            binding.buttonDaysVisibility.text = requireContext().getString(R.string.button_week_show_weekdays)
        }
    }

    override fun hideUpdateProgressBar() {
        binding.recyclerViewDays.animate()
            .translationY(0f)
            .setDuration(200)
            .start()
        binding.progressbarUpdate.animate()
            .translationY(-binding.progressbarUpdate.height.toFloat())
            .setDuration(200)
            .setListener(object : SimpleAnimatorListener {
                override fun onAnimationEnd(animation: Animator) {
                    binding.progressbarUpdate.animate()?.setListener(null)
                    binding.progressbarUpdate.visibility = View.INVISIBLE
                }
            })
            .start()
    }

    override fun showUpdateProgressBar() {
        binding.progressbarUpdate.visibility = View.VISIBLE
        binding.recyclerViewDays.animate()
            .translationY(binding.progressbarUpdate.height.toFloat())
            .setDuration(200)
            .start()
        binding.progressbarUpdate.y = -binding.progressbarUpdate.height.toFloat()
        binding.progressbarUpdate.animate()
            .translationY(0f)
            .setDuration(200)
            .start()
    }

    override fun hideInitProgressBar() {
        binding.progressbarInit.visibility = View.GONE
    }

    override fun showInitProgressBar() {
        binding.progressbarInit.visibility = View.VISIBLE
    }

    override fun showUpdateSuccessMessage() {
        Handler(Looper.getMainLooper()).postDelayed(ToastRunnable(context), 250)
    }

    private class ToastRunnable(context: Context?) : Runnable {
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

    override fun showError(t: Throwable, shouldSetupViews: Boolean) {
        if (weekId == INVALID_VALUE) return
        if (shouldSetupViews) {
            presenter.getSchedule(weekId, shouldUpdateAdapter = true)
        }
        val baseFABMain = requireActivity().findViewById<FloatingActionButton>(R.id.fab_base)
        val snackbar = Snackbar.make(requireView(), AndroidUtils.getErrorMessageResId(t), 5000)
            .setAnchorView(baseFABMain)
            .setAction(R.string.snackbar_week_init_retry) {
                context ?: return@setAction
                if (AndroidUtils.isNetworkAccessible(requireContext().applicationContext)) {
                    layoutManagerSavedState = binding.recyclerViewDays.layoutManager?.onSaveInstanceState()
                    presenter.updateSchedule(weekId)
                } else {
                    showError(t, shouldSetupViews = false)
                }
            }
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
        snackbar.show()
    }

    override fun updateNotesStatus(weekdayId: Int, hasNotes: Boolean) {
        adapter.updateWeekdayNotesStatus(weekdayId, hasNotes)
    }

    companion object {
        private const val ARG_WEEK_ID = "weekID"
        private const val ARG_IS_CURRENT_WEEK = "isCurrentWeek"

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
