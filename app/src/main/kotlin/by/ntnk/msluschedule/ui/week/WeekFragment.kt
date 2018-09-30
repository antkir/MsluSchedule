package by.ntnk.msluschedule.ui.week

import android.animation.Animator
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.Toast
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.mvp.views.MvpFragment
import by.ntnk.msluschedule.ui.adapters.LessonRecyclerViewAdapter
import by.ntnk.msluschedule.ui.adapters.VIEWTYPE_STUDYGROUP
import by.ntnk.msluschedule.ui.adapters.VIEWTYPE_TEACHER
import by.ntnk.msluschedule.ui.adapters.VIEWTYPE_WEEKDAY
import by.ntnk.msluschedule.ui.lessoninfo.LessonInfoActivity
import by.ntnk.msluschedule.ui.weekday.WeekdayActivity
import by.ntnk.msluschedule.utils.*
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_week.*
import javax.inject.Inject

class WeekFragment : MvpFragment<WeekPresenter, WeekView>(), WeekView {
    private lateinit var recyclerView: RecyclerView
    private lateinit var smoothScroller: RecyclerView.SmoothScroller
    private var isEmptyScheduleDaysVisible: Boolean = false
    private var weekId: Int = INVALID_VALUE
    private var isCurrentWeek: Boolean = false

    override val view: WeekView
        get() = this

    @Inject
    lateinit var injectedPresenter: Lazy<WeekPresenter>

    override fun onCreatePresenter(): WeekPresenter = injectedPresenter.get()

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        weekId = arguments?.getInt(ARG_WEEK_ID) ?: INVALID_VALUE
        isCurrentWeek = arguments?.getBoolean(ARG_IS_CURRENT_WEEK) == true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_week, container, false)
        initRecyclerView(rootView)
        return rootView
    }

    private fun initRecyclerView(rootView: View) {
        recyclerView = rootView.findViewById(R.id.rv_week_days)
        with(recyclerView) {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = LessonRecyclerViewAdapter()
            initAdapterOnClickListener()
        }
        smoothScroller = object : LinearSmoothScroller(context!!) {
            override fun getVerticalSnapPreference(): Int {
                return LinearSmoothScroller.SNAP_TO_START
            }
        }
    }

    private fun initAdapterOnClickListener() {
        val adapter = recyclerView.adapter as LessonRecyclerViewAdapter
        adapter.onClickObservable.subscribeBy(
                onNext = {
                    when (it.viewType) {
                        VIEWTYPE_WEEKDAY -> {
                            val weekdayId = (it as LessonRecyclerViewAdapter.DayView).weekdayId
                            WeekdayActivity.startActivity(context!!, weekdayId)
                        }
                        VIEWTYPE_STUDYGROUP -> {
                            val lessonId = (it as LessonRecyclerViewAdapter.StudyGroupLessonView).lesson.id
                            LessonInfoActivity.startActivity(context!!, lessonId, ScheduleType.STUDYGROUP, weekId)
                        }
                        VIEWTYPE_TEACHER -> {
                            val lessonId = (it as LessonRecyclerViewAdapter.TeacherLessonView).lesson.id
                            LessonInfoActivity.startActivity(context!!, lessonId, ScheduleType.TEACHER, weekId)
                        }
                    }
                }
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (weekId == INVALID_VALUE) return
        presenter.getSchedule(weekId)
    }

    override fun onStart() {
        super.onStart()
        presenter.getNotesStatus()
    }

    fun showToday() {
        if (text_week_nolessons.visibility != View.VISIBLE) {
            val adapter = recyclerView.adapter as LessonRecyclerViewAdapter
            val index = adapter.getWeekDayViewIndex(presenter.getCurrentDayOfWeek())
            smoothScroller.targetPosition = index
            recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
        }
    }

    override fun showSchedule(data: List<WeekdayWithLessons<Lesson>>) {
        if (data.map { it.lessons.size }.sum() == 0) {
            button_week_weekdays_visibility.visibility = View.VISIBLE
            if (!isEmptyScheduleDaysVisible) {
                text_week_nolessons.visibility = View.VISIBLE
                rv_week_days.setOnTouchListener { _, _ -> true }
            } else {
                text_week_nolessons.visibility = View.GONE
                rv_week_days.setOnTouchListener(null)
            }
            rv_week_days.addOnScrollListener(recyclerViewScrollListener)
            button_week_weekdays_visibility.setOnClickListener { initButtonWeekdays() }
        } else {
            button_week_weekdays_visibility.visibility = View.GONE
            text_week_nolessons.visibility = View.GONE
            rv_week_days.setOnTouchListener(null)
            rv_week_days.removeOnScrollListener(recyclerViewScrollListener)
        }

        val adapter = recyclerView.adapter as LessonRecyclerViewAdapter
        adapter.initData(data)

        if (isRecentlyCreated && isCurrentWeek) {
            val index = adapter.getWeekDayViewIndex(presenter.getCurrentDayOfWeek())
            recyclerView.layoutManager?.scrollToPosition(index)
        }

        if (isCurrentWeek) {
            activity?.findViewById<ProgressBar>(R.id.progressbar_main)?.visibility = View.GONE
            parentFragment?.view?.findViewById<ViewPager>(R.id.viewpager_weekscontainer)?.visibility = View.VISIBLE
        }
    }

    private val recyclerViewScrollListener: RecyclerView.OnScrollListener =
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    if (button_week_weekdays_visibility.visibility == View.VISIBLE && dy > 0) {
                        button_week_weekdays_visibility.visibility = View.GONE
                        val anim = AnimationUtils.loadAnimation(
                                button_week_weekdays_visibility.context,
                                R.anim.button_week_slide_down
                        )
                        button_week_weekdays_visibility.startAnimation(anim)
                    } else if (button_week_weekdays_visibility.visibility != View.VISIBLE && dy < 0) {
                        button_week_weekdays_visibility.visibility = View.VISIBLE
                        val anim = AnimationUtils.loadAnimation(
                                button_week_weekdays_visibility.context,
                                R.anim.button_week_slide_up
                        )
                        button_week_weekdays_visibility.startAnimation(anim)
                    }
                }
            }

    private fun initButtonWeekdays() {
        if (!isEmptyScheduleDaysVisible) {
            isEmptyScheduleDaysVisible = true
            text_week_nolessons.visibility = View.GONE

            button_week_weekdays_visibility.text = context!!.getString(R.string.button_week_hide_weekdays)
            rv_week_days.setOnTouchListener(null)
        } else {
            isEmptyScheduleDaysVisible = false
            text_week_nolessons.visibility = View.VISIBLE

            // workaround the edge case, when the button is pressed during
            // the hiding animation and won't show again
            rv_week_days.smoothScrollBy(0, -1)

            button_week_weekdays_visibility.text = context!!.getString(R.string.button_week_show_weekdays)
            rv_week_days.setOnTouchListener { _, _ -> true }
        }
    }

    override fun hideUpdateProgressBar() {
        rv_week_days.animate()
                .translationY(0f)
                .setDuration(200)
                .start()
        progressbar_week.animate()
                .translationY(-progressbar_week.height.toFloat())
                .setDuration(200)
                .setListener(object : SimpleAnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        progressbar_week.animate().setListener(null)
                        progressbar_week.visibility = View.INVISIBLE
                    }
                })
                .start()
    }

    override fun showUpdateProgressBar() {
        progressbar_week.visibility = View.VISIBLE
        rv_week_days.animate()
                .translationY(progressbar_week.height.toFloat())
                .setDuration(200)
                .start()
        progressbar_week.y = -progressbar_week.height.toFloat()
        progressbar_week.animate()
                .translationY(0f)
                .setDuration(200)
                .start()
    }

    override fun hideInitProgressBar() {
        progressbar_week_init.visibility = View.GONE
    }

    override fun showInitProgressBar() {
        progressbar_week_init.visibility = View.VISIBLE
    }

    override fun showUpdateSuccessMessage() {
        Toast.makeText(
                context,
                resources.getString(R.string.messsage_schedule_update_successful),
                Toast.LENGTH_SHORT
        ).show()
    }

    override fun showError(t: Throwable, shouldSetupViews: Boolean) {
        if (weekId == INVALID_VALUE) return
        if (shouldSetupViews) {
            presenter.getSchedule(weekId)
        }
        Snackbar.make(getView()!!, getErrorMessageResId(t), 5000)
                .setAction(R.string.snackbar_week_init_retry) {
                    if (isNetworkAccessible(context!!.applicationContext)) {
                        presenter.updateSchedule(weekId)
                    } else {
                        showError(t, shouldSetupViews = false)
                    }
                }
                .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.fragment_week_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.item_week_update -> {
                if (isNetworkAccessible(context!!.applicationContext)) {
                    if (weekId == INVALID_VALUE) return true
                    presenter.updateSchedule(weekId)
                } else {
                    showSnackbarNetworkInaccessible(getView()!!)
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun updateNotesStatus(weekdayId: Int, hasNotes: Boolean) {
        val adapter = recyclerView.adapter as LessonRecyclerViewAdapter
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
