package by.ntnk.msluschedule.ui.adapters

import android.content.Context
import android.graphics.Rect
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.utils.*
import java.util.ArrayList

private const val VIEWTYPE_BLANK = R.layout.item_blanklesson
private const val VIEWTYPE_STUDYGROUP = R.layout.item_studygrouplesson
private const val VIEWTYPE_TEACHER = R.layout.item_teacherlesson
private const val VIEWTYPE_WEEKDAY = R.layout.item_day
private const val VIEWTYPE_DAYEND = R.layout.item_dayend

class LessonRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val data = ArrayList<LessonView>()

    @Throws(NullPointerException::class)
    fun initData(days: List<WeekdayWithLessons<Lesson>>) {
        data.clear()
        for (day in days) {
            data.add(DayLessonView(day.weekday))
            if (day.lessons.isNotEmpty()) {
                for (lesson in day.lessons) {
                    val lessonLesson: LessonView =
                            when (lesson) {
                                is StudyGroupLesson -> StudyGroupLessonView(lesson)
                                is TeacherLesson -> TeacherLessonView(lesson)
                                else -> throw NullPointerException()
                            }
                    data.add(lessonLesson)
                }
            } else {
                data.add(BlankLessonView())
            }
            data.add(DayEndView())
        }
        notifyDataSetChanged()
    }

    fun getWeekDayViewIndex(weekdayNumber: Int): Int {
        var dayIndex = 0
        for (i in 0 until data.size) {
            if (data[i] is DayLessonView) {
                if (dayIndex == weekdayNumber - 1) {
                    return i
                }
                ++dayIndex
            }
        }
        return 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEWTYPE_STUDYGROUP -> {
                val view = inflater.inflate(R.layout.item_studygrouplesson, parent, false)
                StudyGroupLessonViewHolder(view)
            }
            VIEWTYPE_TEACHER -> {
                val view = inflater.inflate(R.layout.item_teacherlesson, parent, false)
                TeacherLessonViewHolder(view)
            }
            VIEWTYPE_WEEKDAY -> {
                val view = inflater.inflate(R.layout.item_day, parent, false)
                DayViewHolder(view)
            }
            VIEWTYPE_DAYEND -> {
                val view = inflater.inflate(R.layout.item_dayend, parent, false)
                DayEndViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_blanklesson, parent, false)
                BlankViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        data[position].bindViewHolder(viewHolder)
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int = data[position].viewType

    private class BlankLessonView : LessonView {
        override val viewType: Int = VIEWTYPE_BLANK

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) = Unit
    }

    private class DayEndView : LessonView {
        override val viewType: Int = VIEWTYPE_DAYEND

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) = Unit
    }

    private class DayLessonView(private val weekday: String) : LessonView {
        override val viewType: Int = VIEWTYPE_WEEKDAY

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as DayViewHolder) {
                weekDay.text = weekdayFromTag(weekday, itemView.context)
            }
        }

        @Throws(NullPointerException::class)
        private fun weekdayFromTag(weekDayTag: String, context: Context): String {
            return when (weekDayTag) {
                MONDAY -> context.resources.getString(R.string.monday)
                TUESDAY -> context.resources.getString(R.string.tuesday)
                WEDNESDAY -> context.resources.getString(R.string.wednesday)
                THURSDAY -> context.resources.getString(R.string.thursday)
                FRIDAY -> context.resources.getString(R.string.friday)
                SATURDAY -> context.resources.getString(R.string.saturday)
                SUNDAY -> context.resources.getString(R.string.sunday)
                else -> throw NullPointerException()
            }
        }
    }

    private class StudyGroupLessonView(private val lesson: StudyGroupLesson) : LessonView {
        override val viewType: Int = VIEWTYPE_STUDYGROUP

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as StudyGroupLessonViewHolder) {
                lessonStartTime.text = lesson.startTime
                lessonEndTime.text = lesson.endTime
                lessonSubject.text = lesson.subject
                lessonTeacher.text = lesson.teacher
                lessonClassroom.text = lesson.classroom
            }
        }
    }

    private class TeacherLessonView(private val lesson: TeacherLesson) : LessonView {
        override val viewType: Int = VIEWTYPE_TEACHER

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as TeacherLessonViewHolder) {
                lessonStartTime.text = lesson.startTime
                lessonEndTime.text = lesson.endTime
                lessonSubject.text = lesson.subject
                lessonGroups.text = lesson.groups
                lessonClassroom.text = lesson.classroom
                lessonType.text = lesson.type
                lessonFaculty.text = lesson.faculty

                if (lesson.subject.isBlank()) {
                    val typeBgColor = ContextCompat.getColor(itemView.context, R.color.surface)
                    lessonType.setBackgroundColor(typeBgColor)
                }
            }
        }
    }

    private interface LessonView {
        val viewType: Int
        fun bindViewHolder(viewHolder: RecyclerView.ViewHolder)
    }

    private class StudyGroupLessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lessonStartTime: TextView = view.findViewById(R.id.text_studygrouplesson_starttime)
        val lessonEndTime: TextView = view.findViewById(R.id.text_studygrouplesson_endtime)
        val lessonSubject: TextView = view.findViewById(R.id.text_studygrouplesson_subject)
        val lessonTeacher: TextView = view.findViewById(R.id.text_studygrouplesson_teacher)
        val lessonClassroom: TextView = view.findViewById(R.id.text_studygrouplesson_classroom)
    }

    private class TeacherLessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lessonStartTime: TextView = view.findViewById(R.id.text_teacherlesson_starttime)
        val lessonEndTime: TextView = view.findViewById(R.id.text_teacherlesson_endtime)
        val lessonSubject: TextView = view.findViewById(R.id.text_teacherlesson_subject)
        val lessonGroups: TextView = view.findViewById(R.id.text_teacherlesson_groups)
        val lessonType: TextView = view.findViewById(R.id.text_teacherlesson_type)
        val lessonFaculty: TextView = view.findViewById(R.id.text_teacherlesson_faculty)
        val lessonClassroom: TextView = view.findViewById(R.id.text_teacherlesson_classroom)
    }

    private class DayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val weekDay: TextView = view.findViewById(R.id.text_day)
    }

    private class BlankViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private class DayEndViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class Divider(context: Context?, orientation: Int) : DividerItemDecoration(context, orientation) {
        override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
            val viewHolder = parent!!.getChildViewHolder(view)
            if (viewHolder is LessonRecyclerViewAdapter.DayEndViewHolder) {
                super.getItemOffsets(outRect, view, parent, state)
            }
        }
    }
}
