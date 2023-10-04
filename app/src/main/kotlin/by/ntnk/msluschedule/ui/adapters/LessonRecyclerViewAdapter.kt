package by.ntnk.msluschedule.ui.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.StudyGroupLesson
import by.ntnk.msluschedule.data.TeacherLesson
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.BaseRVItemView
import by.ntnk.msluschedule.utils.Days
import by.ntnk.msluschedule.utils.EMPTY_STRING
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.ArrayList

const val VIEWTYPE_STUDYGROUP = 101
const val VIEWTYPE_TEACHER = 102
const val VIEWTYPE_WEEKDAY = 103
private const val VIEWTYPE_BLANK = 104
private const val VIEWTYPE_DAYEND = 105

class LessonRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val data = mutableListOf<BaseRVItemView>()
    private val onClickSubject = PublishSubject.create<BaseRVItemView>()

    val onClickObservable: Observable<BaseRVItemView>
        get() = onClickSubject

    @Throws(NullPointerException::class)
    fun initData(days: List<WeekdayWithLessons<Lesson>>) {
        val dataWasEmpty = data.isEmpty()

        val hasNotesList = ArrayList<Boolean>(7)
        for (view in data) {
            if (view is DayView) {
                hasNotesList.add(view.hasNotes)
            }
        }
        data.clear()

        var hasNotesIdx = 0
        for (day in days) {
            val dayView = DayView(day.weekdayId, day.weekday)
            if (hasNotesList.size == Days.num()) {
                dayView.hasNotes = hasNotesList[hasNotesIdx]
                hasNotesIdx++
            }
            data.add(dayView)

            if (day.lessons.isNotEmpty()) {
                var prevStartTime: String = EMPTY_STRING

                for (lesson in day.lessons) {
                    val lessonView: BaseRVItemView =
                        when (lesson) {
                            is StudyGroupLesson -> {
                                val isClassAlternative: Boolean = prevStartTime == lesson.startTime
                                StudyGroupLessonView(lesson, isClassAlternative)
                            }
                            is TeacherLesson -> TeacherLessonView(lesson)
                            else -> throw NullPointerException()
                        }
                    data.add(lessonView)

                    prevStartTime = lesson.startTime
                }
            } else {
                data.add(BlankLessonView())
            }
            data.add(DayEndView())
        }

        if (dataWasEmpty) {
            notifyItemRangeInserted(0, data.size)
        } else {
            @Suppress("NotifyDataSetChanged")
            notifyDataSetChanged()
        }
    }

    fun getWeekdayViewIndex(weekdayNumber: Int): Int {
        var dayIdx = 0
        for (i in 0 until data.size) {
            if (data[i].viewType == VIEWTYPE_WEEKDAY) {
                if (dayIdx == weekdayNumber - 1) {
                    return i
                }
                ++dayIdx
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
                        if (adapterPosition != NO_POSITION &&
                            (data[adapterPosition] as StudyGroupLessonView).lesson.subject != EMPTY_STRING
                        ) {
                            onClickSubject.onNext(data[adapterPosition])
                        }
                    }

                    itemView.setOnLongClickListener {
                        if (adapterPosition != NO_POSITION &&
                            (data[adapterPosition] as StudyGroupLessonView).isClassAlternative
                        ) {
                            if (itemView.context != null) {
                                Toast.makeText(
                                    itemView.context,
                                    itemView.context.resources.getString(R.string.class_same_time_description),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        return@setOnLongClickListener true
                    }
                }
            }
            VIEWTYPE_TEACHER -> {
                val view = inflater.inflate(R.layout.item_teacherlesson, parent, false)
                TeacherLessonViewHolder(view).apply {
                    itemView.setOnClickListener {
                        if (adapterPosition != NO_POSITION &&
                            (data[adapterPosition] as TeacherLessonView).lesson.subject != EMPTY_STRING
                        ) {
                            onClickSubject.onNext(data[adapterPosition])
                        }
                    }
                }
            }
            VIEWTYPE_WEEKDAY -> {
                val view = inflater.inflate(R.layout.item_day, parent, false)
                DayViewHolder(view).apply {
                    itemView.setOnClickListener {
                        if (adapterPosition != NO_POSITION) {
                            onClickSubject.onNext(data[adapterPosition])
                        }
                    }
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
            }
            else -> Unit
        }
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int = data[position].viewType

    private class BlankLessonView : BaseRVItemView {
        override val viewType: Int = VIEWTYPE_BLANK

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) = Unit
    }

    private class DayEndView : BaseRVItemView {
        override val viewType: Int = VIEWTYPE_DAYEND

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) = Unit
    }

    class DayView(val weekdayId: Int, private val weekdayTag: String) : BaseRVItemView {
        internal var hasNotes: Boolean = false

        override val viewType: Int = VIEWTYPE_WEEKDAY

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as DayViewHolder) {
                weekday.text = AndroidUtils.getWeekdayFromTag(weekdayTag, itemView.context)
                val blendMode = if (hasNotes) BlendModeCompat.SRC_IN else BlendModeCompat.DST_IN
                notesIconDrawable?.mutate()?.colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                        accentColor,
                        blendMode
                    )
                notesIcon.setImageDrawable(notesIconDrawable)
            }
        }
    }

    class StudyGroupLessonView(val lesson: StudyGroupLesson, val isClassAlternative: Boolean) : BaseRVItemView {
        override val viewType: Int = VIEWTYPE_STUDYGROUP

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as StudyGroupLessonViewHolder) {
                lessonStartTime.text = if (isClassAlternative) EMPTY_STRING else lesson.startTime
                lessonEndTime.text = if (isClassAlternative) EMPTY_STRING else lesson.endTime
                lessonSubject.text = lesson.subject
                lessonTeacher.text = lesson.teacher
                lessonClassroom.text = lesson.classroom
                lessonType.text = lesson.type.lowercase()

                if (isClassAlternative) {
                    lessonImgTime.visibility = View.VISIBLE
                } else {
                    lessonImgTime.visibility = View.GONE
                }

                if (lessonType.text.isBlank()) {
                    lessonType.visibility = View.GONE
                } else {
                    lessonType.visibility = View.VISIBLE
                }
            }
        }
    }

    class TeacherLessonView(val lesson: TeacherLesson) : BaseRVItemView {
        override val viewType: Int = VIEWTYPE_TEACHER

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as TeacherLessonViewHolder) {
                lessonStartTime.text = lesson.startTime
                lessonEndTime.text = lesson.endTime
                lessonSubject.text = lesson.subject
                lessonGroups.text = lesson.groups
                lessonClassroom.text = lesson.classroom
                lessonType.text = lesson.type.lowercase()
                lessonFaculty.text = lesson.faculty

                if (lessonType.text.isBlank()) {
                    lessonType.visibility = View.GONE
                } else {
                    lessonType.visibility = View.VISIBLE
                }
            }
        }
    }

    private class StudyGroupLessonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val lessonImgTime: ImageView = view.findViewById(R.id.img_studygrouplesson_same_time)
        val lessonStartTime: TextView = view.findViewById(R.id.text_studygrouplesson_starttime)
        val lessonEndTime: TextView = view.findViewById(R.id.text_studygrouplesson_endtime)
        val lessonSubject: TextView = view.findViewById(R.id.text_studygrouplesson_subject)
        val lessonTeacher: TextView = view.findViewById(R.id.text_studygrouplesson_teacher)
        val lessonType: TextView = view.findViewById(R.id.text_studygrouplesson_type)
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
        val weekday: TextView = view.findViewById(R.id.text_day)
        val notesIcon: ImageView = view.findViewById(R.id.imageview_notes_day)
        val notesIconDrawable: Drawable? = ContextCompat.getDrawable(view.context, R.drawable.ic_note_day)
        val accentColor = ContextCompat.getColor(view.context, R.color.colorAccent)
    }

    private class BlankViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private class DayEndViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
