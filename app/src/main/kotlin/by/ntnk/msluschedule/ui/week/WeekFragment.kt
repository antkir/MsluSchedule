package by.ntnk.msluschedule.ui.week

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.mvp.views.MvpFragment
import by.ntnk.msluschedule.ui.adapters.*
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
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
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.setHasFixedSize(true)
        lessonRVAdapter = LessonRecyclerViewAdapter()
        recyclerView.adapter = lessonRVAdapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        weekId ?: return
        presenter.getScheduleData(weekId!!)
    }

    override fun showSchedule(data: List<WeekdayWithLessons<Lesson>>) {
        lessonRVAdapter.initData(data)
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
