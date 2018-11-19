package by.ntnk.msluschedule.ui.adapters

import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.WEEKDAYS_NUMBER
import by.ntnk.msluschedule.utils.getWeekdayFromTag
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.ArrayList

const val VIEWTYPE_STUDYGROUP = R.layout.item_studygrouplesson
const val VIEWTYPE_TEACHER = R.layout.item_teacherlesson
const val VIEWTYPE_WEEKDAY = R.layout.item_day
private const val VIEWTYPE_BLANK = R.layout.item_blanklesson
private const val VIEWTYPE_DAYEND = R.layout.item_dayend

class LessonRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val data = ArrayList<LessonView>()
    private val onClickSubject = PublishSubject.create<LessonView>()

    val onClickObservable: Observable<LessonView>
        get() = onClickSubject

    @Throws(NullPointerException::class)
    fun initData(days: List<WeekdayWithLessons<Lesson>>) {
        val dataWasEmpty = data.isEmpty()

        val hasNotesList = ArrayList<Boolean>()
        for (view in data) {
            if (view is DayView) {
                hasNotesList.add(view.hasNotes)
            }
        }

        data.clear()

        var hasNotesListCounter = 0
        for (day in days) {
            val dayView = DayView(day.weekdayId, day.weekday)
            if (hasNotesList.size == WEEKDAYS_NUMBER) {
                dayView.hasNotes = hasNotesList[hasNotesListCounter]
                hasNotesListCounter++
            }
            data.add(dayView)

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

        if (dataWasEmpty) {
            notifyItemRangeInserted(0, data.size)
        } else {
            notifyDataSetChanged()
        }
    }

    fun getWeekdayViewIndex(weekdayNumber: Int): Int {
        var dayIndex = 0
        for (i in 0 until data.size) {
            if (data[i] is DayView) {
                if (dayIndex == weekdayNumber - 1) {
                    return i
                }
                ++dayIndex
            }
        }
        return 0
    }

    fun updateWeekdayNotesStatus(weekdayId: Int, hasNotes: Boolean) {
        for (i in 0 until data.size) {
            val view = data[i]
            if (view is DayView && view.weekdayId == weekdayId) {
                val hadNotes = view.hasNotes
                if (hadNotes != hasNotes) {
                    view.hasNotes = hasNotes
                    notifyItemChanged(i, hasNotes)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEWTYPE_STUDYGROUP -> {
                val view = inflater.inflate(R.layout.item_studygrouplesson, parent, false)
                StudyGroupLessonViewHolder(view).apply {
                    itemView.setOnClickListener {
                        if ((data[adapterPosition] as StudyGroupLessonView).lesson.subject != EMPTY_STRING) {
                            onClickSubject.onNext(data[adapterPosition])
                        }
                    }
                }
            }
            VIEWTYPE_TEACHER -> {
                val view = inflater.inflate(R.layout.item_teacherlesson, parent, false)
                TeacherLessonViewHolder(view).apply {
                    itemView.setOnClickListener {
                        if ((data[adapterPosition] as TeacherLessonView).lesson.subject != EMPTY_STRING) {
                            onClickSubject.onNext(data[adapterPosition])
                        }
                    }
                }
            }
            VIEWTYPE_WEEKDAY -> {
                val view = inflater.inflate(R.layout.item_day, parent, false)
                DayViewHolder(view).apply {
                    itemView.setOnClickListener { onClickSubject.onNext(data[adapterPosition]) }
                }
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
        when (viewHolder.itemViewType) {
            VIEWTYPE_WEEKDAY -> {
                val surfaceColor = ContextCompat.getColor(viewHolder.itemView.context, R.color.surface)
                viewHolder.itemView.setBackgroundColor(surfaceColor)
                viewHolder.itemView.setOnClickListener { onClickSubject.onNext(data[position]) }
            }
            else -> Unit
        }
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

    class DayView(val weekdayId: Int, private val weekdayTag: String) : LessonView {
        internal var hasNotes: Boolean = false

        override val viewType: Int = VIEWTYPE_WEEKDAY

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as DayViewHolder) {
                weekDay.text = getWeekdayFromTag(weekdayTag, itemView.context)
                if (hasNotes) {
                    notesIconDrawable?.mutate()?.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
                } else {
                    notesIconDrawable?.mutate()?.setColorFilter(accentColor, PorterDuff.Mode.DST_IN)
                }
                notesIcon.setImageDrawable(notesIconDrawable)
            }
        }
    }

    class StudyGroupLessonView(val lesson: StudyGroupLesson) : LessonView {
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

    class TeacherLessonView(val lesson: TeacherLesson) : LessonView {
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
                    lessonType.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.surface))
                } else {
                    lessonType.background = ContextCompat.getDrawable(itemView.context, R.drawable.classtype_circle)
                }
            }
        }
    }

    interface LessonView {
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
        val notesIcon: ImageView = view.findViewById(R.id.imageview_notes_day)
        val notesIconDrawable: Drawable? = ContextCompat.getDrawable(view.context, R.drawable.ic_note_day)
        val accentColor = ContextCompat.getColor(view.context, R.color.colorAccent)
    }

    private class BlankViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private class DayEndViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
