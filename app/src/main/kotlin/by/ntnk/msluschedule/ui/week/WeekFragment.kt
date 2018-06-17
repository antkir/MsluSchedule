package by.ntnk.msluschedule.ui.week

import android.animation.Animator
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.view.animation.AnimationUtils
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.mvp.views.MvpFragment
import by.ntnk.msluschedule.ui.adapters.LessonRecyclerViewAdapter
import by.ntnk.msluschedule.utils.SimpleAnimatorListener
import by.ntnk.msluschedule.utils.isNetworkAccessible
import by.ntnk.msluschedule.utils.showSnackbarNetworkInaccessible
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_week.*
import javax.inject.Inject

class WeekFragment :
        MvpFragment<WeekPresenter, WeekView>(),
        WeekView {
    private var weekId: Int? = null
    private lateinit var lessonRVAdapter: LessonRecyclerViewAdapter

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
        weekId = arguments?.getInt(WEEK_ID)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_week, container, false)
        initRecyclerView(rootView)
        return rootView
    }

    private fun initRecyclerView(rootView: View) {
        val recyclerView: RecyclerView = rootView.findViewById(R.id.rv_week_days)
        val layoutManager = LinearLayoutManager(activity)
        recyclerView.layoutManager = layoutManager
        recyclerView.setHasFixedSize(true)
        lessonRVAdapter = LessonRecyclerViewAdapter()
        recyclerView.adapter = lessonRVAdapter
        val itemDivider =
                LessonRecyclerViewAdapter.Divider(recyclerView.context, layoutManager.orientation)
        recyclerView.addItemDecoration(itemDivider)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        weekId ?: return
        presenter.getSchedule(weekId!!)
    }

    private var isEmptyScheduleDaysVisible: Boolean = false

    override fun showSchedule(data: List<WeekdayWithLessons<Lesson>>) {
        if (data.map { it.lessons.size }.sum() <= 0) {
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

                    button_week_weekdays_visibility.text =
                            context!!.getString(R.string.button_week_hide_weekdays)
                    rv_week_days.setOnTouchListener(null)
                } else {
                    isEmptyScheduleDaysVisible = false
                    text_week_nolessons.visibility = View.VISIBLE

                    // workaround the edge case, when the button is pressed during
                    // the hiding animation and won't show again
                    rv_week_days.smoothScrollBy(0, -1)

                    button_week_weekdays_visibility.text =
                            context!!.getString(R.string.button_week_show_weekdays)
                    rv_week_days.setOnTouchListener { _, _ -> true }
                }
            }
        }
        lessonRVAdapter.initData(data)
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

    override fun showError() {
        if (!isFragmentVisible) return
        Snackbar.make(getView()!!, R.string.error_init_schedule, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snackbar_week_init_retry) {
                    if (isNetworkAccessible(context!!.applicationContext)) {
                        presenter.updateSchedule(weekId!!)
                    } else {
                        showError()
                    }
                }
                .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.fragment_week_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        super.onOptionsItemSelected(item)
        return when (item?.itemId) {
            R.id.item_week_update -> {
                weekId ?: return true
                if (isNetworkAccessible(context!!.applicationContext)) {
                    presenter.updateSchedule(weekId!!)
                } else {
                    showSnackbarNetworkInaccessible(getView()!!)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val WEEK_ID = "weekID"

        fun newInstance(weekId: Int): WeekFragment {
            return WeekFragment().apply {
                arguments = Bundle().apply {
                    putInt(WEEK_ID, weekId)
                }
            }
        }
    }
}
