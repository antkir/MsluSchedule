package by.ntnk.msluschedule.ui.lessoninfo

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NavUtils
import androidx.core.view.WindowInsetsControllerCompat
import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.databinding.ActivityLessoninfoStudygroupBinding
import by.ntnk.msluschedule.databinding.ActivityLessoninfoTeacherBinding
import by.ntnk.msluschedule.mvp.views.MvpActivity
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.INVALID_VALUE
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.ScheduleTypeConverter
import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

class LessonInfoActivity :
    MvpActivity<LessonInfoPresenter, LessonInfoView>(),
    LessonInfoView,
    HasAndroidInjector {

    private var lessonId: Int = INVALID_VALUE
    private var weekId: Int = INVALID_VALUE
    private var scheduleType: ScheduleType? = null

    private lateinit var bindingStudyGroup: ActivityLessoninfoStudygroupBinding
    private lateinit var bindingTeacher: ActivityLessoninfoTeacherBinding

    override val view: LessonInfoView
        get() = this

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var injectedPresenter: Lazy<LessonInfoPresenter>

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    override fun onCreatePresenter(): LessonInfoPresenter = injectedPresenter.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        bindingStudyGroup = ActivityLessoninfoStudygroupBinding.inflate(layoutInflater)
        bindingTeacher = ActivityLessoninfoTeacherBinding.inflate(layoutInflater)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lessonId = intent?.getIntExtra(ARG_LESSON_ID, INVALID_VALUE) ?: INVALID_VALUE
        weekId = intent?.getIntExtra(ARG_WEEK_ID, INVALID_VALUE) ?: INVALID_VALUE
        scheduleType = ScheduleTypeConverter.stringToScheduleType(intent?.getStringExtra(ARG_SCHEDULE_TYPE))

        when (scheduleType) {
            ScheduleType.STUDYGROUP -> setContentView(bindingStudyGroup.root)
            ScheduleType.TEACHER -> setContentView(bindingTeacher.root)
            else -> {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (scheduleType == null || lessonId <= 0 || weekId <= 0) {
            NavUtils.navigateUpFromSameTask(this)
            return
        }
        presenter.getLesson(lessonId, scheduleType!!, weekId)
    }

    override fun showInfo(lesson: StudyGroupLesson, weekdaysWithLesson: List<String>) {
        setFieldText(lesson.subject, bindingStudyGroup.textviewSubject, bindingStudyGroup.imageviewSubject)
        setFieldText(lesson.type, bindingStudyGroup.textviewClassType, bindingStudyGroup.imageviewClassType)
        setFieldText(lesson.teacher, bindingStudyGroup.textviewTeacher, bindingStudyGroup.imageviewTeacher)
        setFieldText(lesson.classroom, bindingStudyGroup.textviewClassroom, bindingStudyGroup.imageviewClassroom)
        setFieldText(
            "${lesson.startTime} - ${lesson.endTime}",
            bindingStudyGroup.textviewTime,
            bindingStudyGroup.imageviewTime
        )

        var weekdays: String = EMPTY_STRING
        for (weekday in weekdaysWithLesson) {
            weekdays += ", ${AndroidUtils.getWeekdayFromTag(weekday, applicationContext)}"
        }
        weekdays = weekdays.drop(2)
        bindingStudyGroup.textviewWeekdays.text = weekdays
    }

    override fun showInfo(lesson: TeacherLesson, weekdaysWithLesson: List<String>) {
        setFieldText(lesson.subject, bindingTeacher.textviewSubject, bindingTeacher.imageviewSubject)
        setFieldText(lesson.classroom, bindingTeacher.textviewClassroom, bindingTeacher.imageviewClassroom)
        setFieldText(
            "${lesson.startTime} - ${lesson.endTime}",
            bindingTeacher.textviewTime,
            bindingTeacher.imageviewTime
        )
        setFieldText(lesson.type, bindingTeacher.textviewClassType, imageView = null)
        val faculties = lesson.faculty.replace(", ", "\n")
        setFieldText(faculties, bindingTeacher.textviewFaculties, imageView = null)
        val groups = lesson.groups.replace(", ", "\n")
        setFieldText(groups, bindingTeacher.textviewGroups, imageView = null)

        if (lesson.type == EMPTY_STRING && lesson.faculty == EMPTY_STRING && lesson.groups == EMPTY_STRING) {
            bindingTeacher.imageviewGroups.visibility = View.GONE
        }

        var weekdays: String = EMPTY_STRING
        for (weekday in weekdaysWithLesson) {
            weekdays += ", ${AndroidUtils.getWeekdayFromTag(weekday, applicationContext)}"
        }
        weekdays = weekdays.drop(2)
        bindingTeacher.textviewWeekdays.text = weekdays
    }

    private fun setFieldText(text: String, textView: TextView, imageView: ImageView?) {
        if (text != EMPTY_STRING) {
            textView.text = text
        } else {
            textView.visibility = View.GONE
            imageView?.visibility = View.GONE
        }
    }

    override fun destroyView() = NavUtils.navigateUpFromSameTask(this)

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
