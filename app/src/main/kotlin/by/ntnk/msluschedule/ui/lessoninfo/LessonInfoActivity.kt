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
import by.ntnk.msluschedule.utils.DEFAULT
import by.ntnk.msluschedule.utils.ScheduleType
import by.ntnk.msluschedule.utils.ScheduleTypeConverter
import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

class LessonInfoActivity :
    MvpActivity<LessonInfoPresenter, LessonInfoView>(),
    LessonInfoView,
    HasAndroidInjector {

    private var classId: Int = Int.DEFAULT
    private var weekId: Int = Int.DEFAULT
    private var scheduleType: ScheduleType? = null
    private val disposables: CompositeDisposable = CompositeDisposable()

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

        classId = intent?.getIntExtra(ARG_CLASS_ID, Int.DEFAULT) ?: Int.DEFAULT
        weekId = intent?.getIntExtra(ARG_WEEK_ID, Int.DEFAULT) ?: Int.DEFAULT
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

        if (scheduleType == null || classId <= 0 || weekId <= 0) {
            Timber.e(IllegalArgumentException())
            NavUtils.navigateUpFromSameTask(this)
            return
        }

        disposables += presenter.classUiStateObservable
            .subscribeBy(
                onNext = { classUiState ->
                    if (classUiState == ClassUiState()) {
                        presenter.getClassDescriptor(classId, scheduleType!!, weekId)
                    }

                    if (classUiState.isData()) {
                        if (classUiState.groupClassDescriptor != null) {
                            onClassDataReceived(classUiState.groupClassDescriptor, classUiState.weekdays)
                        }

                        if (classUiState.teacherClassDescriptor != null) {
                            onClassDataReceived(classUiState.teacherClassDescriptor, classUiState.weekdays)
                        }
                    }

                    if (classUiState.isError()) {
                        Timber.e(classUiState.throwable)
                        NavUtils.navigateUpFromSameTask(this)
                    }
                }
            )
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    private fun onClassDataReceived(classDescriptor: StudyGroupLesson, weekdays: List<String>) {
        setFieldText(classDescriptor.subject, bindingStudyGroup.textviewSubject, bindingStudyGroup.imageviewSubject)
        setFieldText(classDescriptor.type, bindingStudyGroup.textviewClassType, bindingStudyGroup.imageviewClassType)
        setFieldText(classDescriptor.teacher, bindingStudyGroup.textviewTeacher, bindingStudyGroup.imageviewTeacher)
        val classroom = classDescriptor.classroom
        setFieldText(classroom, bindingStudyGroup.textviewClassroom, bindingStudyGroup.imageviewClassroom)
        val time = "${classDescriptor.startTime} - ${classDescriptor.endTime}"
        setFieldText(time, bindingStudyGroup.textviewTime, bindingStudyGroup.imageviewTime)

        var weekdaysText: String = String.DEFAULT
        for (weekday in weekdays) {
            weekdaysText += ", ${AndroidUtils.getWeekdayFromTag(weekday, applicationContext)}"
        }
        weekdaysText = weekdaysText.drop(2)
        bindingStudyGroup.textviewWeekdays.text = weekdaysText
    }

    private fun onClassDataReceived(classDescriptor: TeacherLesson, weekdays: List<String>) {
        setFieldText(classDescriptor.subject, bindingTeacher.textviewSubject, bindingTeacher.imageviewSubject)
        setFieldText(classDescriptor.classroom, bindingTeacher.textviewClassroom, bindingTeacher.imageviewClassroom)
        val time = "${classDescriptor.startTime} - ${classDescriptor.endTime}"
        setFieldText(time, bindingTeacher.textviewTime, bindingTeacher.imageviewTime)
        setFieldText(classDescriptor.type, bindingTeacher.textviewClassType, imageView = null)
        val faculties = classDescriptor.faculty.replace(", ", "\n")
        setFieldText(faculties, bindingTeacher.textviewFaculties, imageView = null)
        val groups = classDescriptor.groups.replace(", ", "\n")
        setFieldText(groups, bindingTeacher.textviewGroups, imageView = null)

        if (classDescriptor.type == String.DEFAULT &&
            classDescriptor.faculty == String.DEFAULT &&
            classDescriptor.groups == String.DEFAULT
        ) {
            bindingTeacher.imageviewGroups.visibility = View.GONE
        }

        var weekdaysText: String = String.DEFAULT
        for (weekday in weekdays) {
            weekdaysText += ", ${AndroidUtils.getWeekdayFromTag(weekday, applicationContext)}"
        }
        weekdaysText = weekdaysText.drop(2)
        bindingTeacher.textviewWeekdays.text = weekdaysText
    }

    private fun setFieldText(text: String, textView: TextView, imageView: ImageView?) {
        if (text != String.DEFAULT) {
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
        private const val ARG_CLASS_ID = "classId"
        private const val ARG_WEEK_ID = "weekId"
        private const val ARG_SCHEDULE_TYPE = "scheduleType"

        fun startActivity(context: Context, lessonId: Int, scheduleType: ScheduleType, weekId: Int) {
            val intent = Intent(context, LessonInfoActivity::class.java).apply {
                putExtra(ARG_CLASS_ID, lessonId)
                putExtra(ARG_WEEK_ID, weekId)
                putExtra(ARG_SCHEDULE_TYPE, ScheduleTypeConverter.scheduleTypeToString(scheduleType))
            }
            context.startActivity(intent)
        }
    }
}
