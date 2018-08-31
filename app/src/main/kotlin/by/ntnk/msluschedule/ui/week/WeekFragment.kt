package by.ntnk.msluschedule.ui.week

import android.animation.Animator
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Toast
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.mvp.views.MvpFragment
import by.ntnk.msluschedule.ui.adapters.LessonRecyclerViewAdapter
import by.ntnk.msluschedule.utils.SimpleAnimatorListener
import by.ntnk.msluschedule.utils.getErrorMessageResId
import by.ntnk.msluschedule.utils.isNetworkAccessible
import by.ntnk.msluschedule.utils.showSnackbarNetworkInaccessible
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_week.*
import javax.inject.Inject

class WeekFragment : MvpFragment<WeekPresenter, WeekView>(), WeekView {
    private lateinit var recyclerView: RecyclerView
    private lateinit var smoothScroller: RecyclerView.SmoothScroller
    private var isEmptyScheduleDaysVisible: Boolean = false
    private var weekId: Int = -1
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
        weekId = arguments?.getInt(ARG_WEEK_ID) ?: -1
        isCurrentWeek = arguments?.getBoolean(ARG_IS_CURRENT_WEEK) == true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_week, container, false)
        initRecyclerView(rootView)
        return rootView
    }

    private fun initRecyclerView(rootView: View) {
        recyclerView = rootView.findViewById(R.id.rv_week_days)
        val itemDivider = LessonRecyclerViewAdapter.Divider(recyclerView.context, RecyclerView.VERTICAL)
        with(recyclerView) {
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
            adapter = LessonRecyclerViewAdapter()
            addItemDecoration(itemDivider)
        }
        smoothScroller = object : LinearSmoothScroller(context!!.applicationContext) {
            override fun getVerticalSnapPreference(): Int {
                return LinearSmoothScroller.SNAP_TO_START
            }
        }
    }

    fun showToday() {
        if (text_week_nolessons.visibility != View.VISIBLE) {
            val adapter = recyclerView.adapter as LessonRecyclerViewAdapter
            val index = adapter.getWeekDayViewIndex(presenter.getCurrentDayOfWeek())
            smoothScroller.targetPosition = index
            recyclerView.layoutManager.startSmoothScroll(smoothScroller)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (weekId == -1) return
        presenter.getSchedule(weekId)
    }

    override fun showSchedule(data: List<WeekdayWithLessons<Lesson>>) {
        if (data.map { it.lessons.size }.sum() == 0) {
            button_week_weekdays_visibility.visibility = View.VISIBLE
            if (!isEmptyScheduleDaysVisible) {
                text_week_nolessons.visibility = View.VISIBLE
                rv_week_days.setOnTouchListener { _, _ -> true }
            } else {
                text_week_nolessons.visibility = View.INVISIBLE
                rv_week_days.setOnTouchListener(null)
            }
            rv_week_days.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    if (button_week_weekdays_visibility.visibility == View.VISIBLE && dy > 0) {
                        button_week_weekdays_visibility.visibility = View.INVISIBLE
                        val anim = AnimationUtils.loadAnimation(
                                button_week_weekdays_visibility.context,
                                R.anim.button_week_slide_down
                        )
                        button_week_weekdays_visibility.startAnimation(anim)
                    } else if (button_week_weekdays_visibility.visibility == View.INVISIBLE && dy < 0) {
                        button_week_weekdays_visibility.visibility = View.VISIBLE
                        val anim = AnimationUtils.loadAnimation(
                                button_week_weekdays_visibility.context,
                                R.anim.button_week_slide_up
                        )
                        button_week_weekdays_visibility.startAnimation(anim)
                    }
                }
            })
            button_week_weekdays_visibility.setOnClickListener {
                if (!isEmptyScheduleDaysVisible) {
                    isEmptyScheduleDaysVisible = true
                    text_week_nolessons.visibility = View.INVISIBLE

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
        } else {
            button_week_weekdays_visibility.visibility = View.GONE
            text_week_nolessons.visibility = View.GONE
            rv_week_days.setOnTouchListener(null)
        }
        val adapter = recyclerView.adapter as LessonRecyclerViewAdapter
        adapter.initData(data)
        if (isCurrentWeek && !isFragmentRecreated) {
            val index = adapter.getWeekDayViewIndex(presenter.getCurrentDayOfWeek())
            recyclerView.layoutManager.scrollToPosition(index)
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
        progressbar_week_init.visibility = View.INVISIBLE
    }

    override fun showInitProgressBar() {
        progressbar_week_init.visibility = View.VISIBLE
    }

    override fun showUpdateSuccessMessage() {
        Toast.makeText(
                context?.applicationContext,
                resources.getString(R.string.messsage_schedule_update_successful),
                Toast.LENGTH_SHORT
        ).show()
    }

    override fun showError(t: Throwable, shouldSetupViews: Boolean) {
        if (weekId == -1) return
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
                    if (weekId == -1) return true
                    presenter.updateSchedule(weekId)
                } else {
                    showSnackbarNetworkInaccessible(getView()!!)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
