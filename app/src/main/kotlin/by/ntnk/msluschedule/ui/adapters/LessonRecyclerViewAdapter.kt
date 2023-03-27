package by.ntnk.msluschedule.ui.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import by.ntnk.msluschedule.databinding.ItemClassBlankBinding
import by.ntnk.msluschedule.databinding.ItemClassStudygroupBinding
import by.ntnk.msluschedule.databinding.ItemClassTeacherBinding
import by.ntnk.msluschedule.databinding.ItemDayBinding
import by.ntnk.msluschedule.databinding.ItemDayEndBinding
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
                                val hasClassOverlap: Boolean = prevStartTime == lesson.startTime
                                StudyGroupLessonView(lesson, hasClassOverlap)
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
        return when (viewType) {
            VIEWTYPE_STUDYGROUP -> {
                val binding = ItemClassStudygroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                StudyGroupClassViewHolder(binding).apply {
                    itemView.setOnClickListener {
                        if (adapterPosition != NO_POSITION &&
                            (data[adapterPosition] as StudyGroupLessonView).lesson.subject != EMPTY_STRING
                        ) {
                            onClickSubject.onNext(data[adapterPosition])
                        }
                    }

                    itemView.setOnLongClickListener {
                        if (adapterPosition != NO_POSITION &&
                            (data[adapterPosition] as StudyGroupLessonView).hasClassOverlap
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
                val binding = ItemClassTeacherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TeacherClassViewHolder(binding).apply {
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
                val binding = ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DayViewHolder(binding).apply {
                    itemView.setOnClickListener {
                        if (adapterPosition != NO_POSITION) {
                            onClickSubject.onNext(data[adapterPosition])
                        }
                    }
                }
            }
            VIEWTYPE_DAYEND -> {
                val binding = ItemDayEndBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DayEndViewHolder(binding)
            }
            else -> {
                val binding = ItemClassBlankBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                BlankViewHolder(binding)
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
                binding.textWeekday.text = AndroidUtils.getWeekdayFromTag(weekdayTag, itemView.context)
                val blendMode = if (hasNotes) BlendModeCompat.SRC_IN else BlendModeCompat.DST_IN
                notesIconDrawable?.mutate()?.colorFilter =
                    BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                        accentColor,
                        blendMode
                    )
                binding.imageNotes.setImageDrawable(notesIconDrawable)
            }
        }
    }

    class StudyGroupLessonView(val lesson: StudyGroupLesson, val hasClassOverlap: Boolean) : BaseRVItemView {
        override val viewType: Int = VIEWTYPE_STUDYGROUP

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as StudyGroupClassViewHolder) {
                binding.textTimeStart.text = if (hasClassOverlap) EMPTY_STRING else lesson.startTime
                binding.textTimeEnd.text = if (hasClassOverlap) EMPTY_STRING else lesson.endTime
                binding.textSubject.text = lesson.subject
                binding.textTeacher.text = lesson.teacher
                binding.textClassroom.text = lesson.classroom
                binding.textClassType.text = lesson.type

                if (hasClassOverlap) {
                    binding.imageClassOverlap.visibility = View.VISIBLE
                } else {
                    binding.imageClassOverlap.visibility = View.GONE
                }

                if (binding.textClassType.text.isBlank()) {
                    binding.textClassType.visibility = View.GONE
                } else {
                    binding.textClassType.visibility = View.VISIBLE
                }
            }
        }
    }

    class TeacherLessonView(val lesson: TeacherLesson) : BaseRVItemView {
        override val viewType: Int = VIEWTYPE_TEACHER

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as TeacherClassViewHolder) {
                binding.textTimeStart.text = lesson.startTime
                binding.textTimeEnd.text = lesson.endTime
                binding.textSubject.text = lesson.subject
                binding.textGroups.text = lesson.groups
                binding.textClassroom.text = lesson.classroom
                binding.textClassType.text = lesson.type
                binding.textFaculty.text = lesson.faculty

                if (binding.textClassType.text.isBlank()) {
                    binding.textClassType.visibility = View.GONE
                } else {
                    binding.textClassType.visibility = View.VISIBLE
                }
            }
        }
    }

    private class StudyGroupClassViewHolder(val binding: ItemClassStudygroupBinding) : RecyclerView.ViewHolder(binding.root)

    private class TeacherClassViewHolder(val binding: ItemClassTeacherBinding) : RecyclerView.ViewHolder(binding.root)

    private class DayViewHolder(val binding: ItemDayBinding) : RecyclerView.ViewHolder(binding.root) {
        val notesIconDrawable: Drawable? = ContextCompat.getDrawable(binding.root.context, R.drawable.ic_note_day)
        val accentColor = ContextCompat.getColor(binding.root.context, R.color.colorAccent)
    }

    private class BlankViewHolder(binding: ItemClassBlankBinding) : RecyclerView.ViewHolder(binding.root)

    private class DayEndViewHolder(binding: ItemDayEndBinding) : RecyclerView.ViewHolder(binding.root)
}
