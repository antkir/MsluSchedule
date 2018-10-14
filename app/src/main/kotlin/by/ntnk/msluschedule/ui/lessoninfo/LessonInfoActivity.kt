package by.ntnk.msluschedule.ui.lessoninfo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.NavUtils
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.mvp.views.MvpActivity
import by.ntnk.msluschedule.utils.*
import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_lessoninfo_studygroup.*
import kotlinx.android.synthetic.main.activity_lessoninfo_teacher.*
import javax.inject.Inject

class LessonInfoActivity : MvpActivity<LessonInfoPresenter, LessonInfoView>(),
        LessonInfoView,
        HasSupportFragmentInjector {
    private var lessonId: Int = INVALID_VALUE
    private var weekId: Int = INVALID_VALUE
    private var scheduleType: ScheduleType? = null

    override val view: LessonInfoView
        get() = this

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var injectedPresenter: Lazy<LessonInfoPresenter>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentDispatchingAndroidInjector

    override fun onCreatePresenter(): LessonInfoPresenter = injectedPresenter.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lessonId = intent?.getIntExtra(ARG_LESSON_ID, INVALID_VALUE) ?: INVALID_VALUE
        weekId = intent?.getIntExtra(ARG_WEEK_ID, INVALID_VALUE) ?: INVALID_VALUE
        scheduleType = ScheduleTypeConverter.stringToScheduleType(intent?.getStringExtra(ARG_SCHEDULE_TYPE))

        when (scheduleType) {
            ScheduleType.STUDYGROUP -> setContentView(R.layout.activity_lessoninfo_studygroup)
            ScheduleType.TEACHER -> setContentView(R.layout.activity_lessoninfo_teacher)
        }
    }

    override fun onStart() {
        super.onStart()
        if (scheduleType == null || lessonId <= 0) {
            NavUtils.navigateUpFromSameTask(this)
            return
        }
        presenter.getLesson(lessonId, scheduleType!!, weekId)
    }

    override fun showInfo(lesson: StudyGroupLesson, weekdaysWithLesson: List<String>) {
        setFieldText(lesson.subject,
                     textview_subject_lessoninfo_studygroup,
                     imageview_subject_lessoninfo_studygroup)
        setFieldText(lesson.teacher,
                     textview_teacher_lessoninfo_studygroup,
                     imageview_teacher_lessoninfo_studygroup)
        setFieldText(lesson.classroom,
                     textview_classroom_lessoninfo_studygroup,
                     imageview_classroom_lessoninfo_studygroup)
        setFieldText(String.format("%s - %s", lesson.startTime, lesson.endTime),
                     textview_time_lessoninfo_studygroup, imageview_time_lessoninfo_studygroup)

        var weekdays: String = EMPTY_STRING
        for (weekday in weekdaysWithLesson) {
            weekdays += String.format(", %s", getWeekdayFromTag(weekday, applicationContext))
        }
        weekdays = weekdays.drop(2)
        textview_weekdays_lessoninfo_studygroup.text = weekdays
    }

    override fun showInfo(lesson: TeacherLesson, weekdaysWithLesson: List<String>) {
        setFieldText(lesson.subject, textview_subject_lessoninfo_teacher, imageview_subject_lessoninfo_teacher)
        setFieldText(lesson.classroom, textview_classroom_lessoninfo_teacher, imageview_classroom_lessoninfo_teacher)
        setFieldText(String.format("%s - %s", lesson.startTime, lesson.endTime),
                     textview_time_lessoninfo_teacher, imageview_time_lessoninfo_teacher)
        setFieldText(lesson.type, textview_type_lessoninfo_teacher, imageView = null)
        val faculties = lesson.faculty.replace(", ", "\n")
        setFieldText(faculties, textview_faculties_lessoninfo_teacher, imageView = null)
        val groups = lesson.groups.replace(", ", "\n")
        setFieldText(groups, textview_groups_lessoninfo_teacher, imageView = null)

        if (lesson.type == EMPTY_STRING && lesson.faculty == EMPTY_STRING && lesson.groups == EMPTY_STRING) {
            imageview_groups_lessoninfo_teacher.visibility = View.GONE
        }

        var weekdays: String = EMPTY_STRING
        for (weekday in weekdaysWithLesson) {
            weekdays += String.format(", %s", getWeekdayFromTag(weekday, applicationContext))
        }
        weekdays = weekdays.drop(2)
        textview_weekdays_lessoninfo_teacher.text = weekdays
    }

    private fun setFieldText(text: String, textView: TextView, imageView: ImageView?) {
        if (text != EMPTY_STRING) {
            textView.text = text
        } else {
            textView.visibility = View.GONE
            imageView?.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val ARG_LESSON_ID = "lessonId"
        private const val ARG_WEEK_ID = "weekId"
        private const val ARG_SCHEDULE_TYPE = "scheduleType"

        fun startActivity(context: Context, lessonId: Int, scheduleType: ScheduleType, weekId: Int) {
            val intent = Intent(context, LessonInfoActivity::class.java).apply {
                putExtra(ARG_LESSON_ID, lessonId)
                putExtra(ARG_WEEK_ID, weekId)
                putExtra(ARG_SCHEDULE_TYPE, ScheduleTypeConverter.scheduleTypeToString(scheduleType))
            }
            context.startActivity(intent)
        }
    }
}
