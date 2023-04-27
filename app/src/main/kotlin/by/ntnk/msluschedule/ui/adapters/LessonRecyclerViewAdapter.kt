package by.ntnk.msluschedule.ui.adapters

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.DiffUtil
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
import by.ntnk.msluschedule.utils.DEFAULT
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LessonRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class ItemViewType {
        STUDYGROUPCLASS,
        TEACHERCLASS,
        DAY,
        DAYEND,
        BLANKDAY;

        fun toInt(): Int = ordinal
    }

    interface BaseItemView {
        val viewType: ItemViewType
        fun bindViewHolder(viewHolder: RecyclerView.ViewHolder)
    }

    private val itemViews = mutableListOf<BaseItemView>()
    private val onClickSubject = PublishSubject.create<BaseItemView>()

    val onClickObservable: Observable<BaseItemView>
        get() = onClickSubject

    fun onNewData(days: List<WeekdayWithLessons<Lesson>>) {
        val oldItemViews = ArrayList(itemViews)
        itemViews.clear()

        for (day in days) {
            itemViews.add(DayItemView(day.weekdayId, day.weekday))
            if (day.lessons.isNotEmpty()) {
                var prevStartTime: String = String.DEFAULT
                for (classDesciptor in day.lessons) {
                    val classItemView: BaseItemView =
                        when (classDesciptor) {
                            is StudyGroupLesson -> {
                                val overlapsPreviousClass: Boolean = prevStartTime == classDesciptor.startTime
                                StudyGroupClassItemView(classDesciptor, day.weekdayId, overlapsPreviousClass)
                            }
                            is TeacherLesson -> TeacherClassItemView(classDesciptor, day.weekdayId)
                            else -> throw RuntimeException("Illegal class type!")
                        }
                    itemViews.add(classItemView)
                    prevStartTime = classDesciptor.startTime
                }
            } else {
                itemViews.add(BlankDayItemView())
            }
            itemViews.add(DayEndItemView())
        }

        if (oldItemViews.isEmpty()) {
            notifyItemRangeInserted(0, itemViews.size)
        } else {
            val callback = DiffUtilCallback(oldItemViews, itemViews)
            val diffResult = DiffUtil.calculateDiff(callback)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    fun getWeekdayViewIndex(weekdayNumber: Int): Int {
        var dayIdx = 0
        for (i in 0 until itemViews.size) {
            if (itemViews[i].viewType == ItemViewType.DAY) {
                if (dayIdx == weekdayNumber - 1) {
                    return i
                }
                ++dayIdx
            }
        }
        return 0
    }

    fun updateWeekdayNotesStatus(weekdayId: Int, hasNotes: Boolean) {
        for (i in 0 until itemViews.size) {
            val itemView = itemViews[i]
            if (itemView is DayItemView && itemView.weekdayId == weekdayId) {
                if (itemView.hasNotes != hasNotes) {
                    itemView.hasNotes = hasNotes
                    notifyItemChanged(i, hasNotes)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemViewType.STUDYGROUPCLASS.toInt() -> {
                val binding = ItemClassStudygroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                StudyGroupClassViewHolder(binding).apply {
                    itemView.setOnClickListener {
                        if (adapterPosition != NO_POSITION &&
                            (itemViews[adapterPosition] as StudyGroupClassItemView).classDesciptor.subject != String.DEFAULT
                        ) {
                            onClickSubject.onNext(itemViews[adapterPosition])
                        }
                    }

                    itemView.setOnLongClickListener {
                        if (adapterPosition != NO_POSITION &&
                            (itemViews[adapterPosition] as StudyGroupClassItemView).overlapsPreviousClass
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
            ItemViewType.TEACHERCLASS.toInt() -> {
                val binding = ItemClassTeacherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                TeacherClassViewHolder(binding).apply {
                    itemView.setOnClickListener {
                        if (adapterPosition != NO_POSITION &&
                            (itemViews[adapterPosition] as TeacherClassItemView).classDesciptor.subject != String.DEFAULT
                        ) {
                            onClickSubject.onNext(itemViews[adapterPosition])
                        }
                    }
                }
            }
            ItemViewType.DAY.toInt() -> {
                val binding = ItemDayBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DayViewHolder(binding).apply {
                    itemView.setOnClickListener {
                        if (adapterPosition != NO_POSITION) {
                            onClickSubject.onNext(itemViews[adapterPosition])
                        }
                    }
                }
            }
            ItemViewType.DAYEND.toInt() -> {
                val binding = ItemDayEndBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DayEndViewHolder(binding)
            }
            else -> {
                val binding = ItemClassBlankBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                BlankDayViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        itemViews[position].bindViewHolder(viewHolder)
        when (viewHolder.itemViewType) {
            ItemViewType.DAY.toInt() -> {
                val surfaceColor = ContextCompat.getColor(viewHolder.itemView.context, R.color.surface)
                viewHolder.itemView.setBackgroundColor(surfaceColor)
                viewHolder.itemView.setOnClickListener { onClickSubject.onNext(itemViews[position]) }
            }
            else -> Unit
        }
    }

    override fun getItemCount(): Int = itemViews.size

    override fun getItemViewType(position: Int): Int = itemViews[position].viewType.toInt()

    fun isEmpty(): Boolean = itemViews.size == 0

    private class BlankDayItemView : BaseItemView {
        override val viewType: ItemViewType = ItemViewType.BLANKDAY

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) = Unit
    }

    private class DayEndItemView : BaseItemView {
        override val viewType: ItemViewType = ItemViewType.DAYEND

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) = Unit
    }

    class DayItemView(val weekdayId: Int, private val weekdayTag: String) : BaseItemView {
        var hasNotes: Boolean = false

        override val viewType: ItemViewType = ItemViewType.DAY

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

    class StudyGroupClassItemView(
        val classDesciptor: StudyGroupLesson,
        val weekdayId: Int,
        val overlapsPreviousClass: Boolean
    ) : BaseItemView {
        override val viewType: ItemViewType = ItemViewType.STUDYGROUPCLASS

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as StudyGroupClassViewHolder) {
                binding.textTimeStart.text = if (overlapsPreviousClass) String.DEFAULT else classDesciptor.startTime
                binding.textTimeEnd.text = if (overlapsPreviousClass) String.DEFAULT else classDesciptor.endTime
                binding.textSubject.text = classDesciptor.subject
                binding.textTeacher.text = classDesciptor.teacher
                binding.textClassroom.text = classDesciptor.classroom
                binding.textClassType.text = classDesciptor.type

                if (overlapsPreviousClass) {
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

    class TeacherClassItemView(val classDesciptor: TeacherLesson, val weekdayId: Int) : BaseItemView {
        override val viewType: ItemViewType = ItemViewType.TEACHERCLASS

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as TeacherClassViewHolder) {
                binding.textTimeStart.text = classDesciptor.startTime
                binding.textTimeEnd.text = classDesciptor.endTime
                binding.textSubject.text = classDesciptor.subject
                binding.textGroups.text = classDesciptor.groups
                binding.textClassroom.text = classDesciptor.classroom
                binding.textClassType.text = classDesciptor.type
                binding.textFaculty.text = classDesciptor.faculty

                if (binding.textClassType.text.isBlank()) {
                    binding.textClassType.visibility = View.GONE
                } else {
                    binding.textClassType.visibility = View.VISIBLE
                }
            }
        }
    }

    private class StudyGroupClassViewHolder(val binding: ItemClassStudygroupBinding) :
        RecyclerView.ViewHolder(binding.root)

    private class TeacherClassViewHolder(val binding: ItemClassTeacherBinding) : RecyclerView.ViewHolder(binding.root)

    private class DayViewHolder(val binding: ItemDayBinding) : RecyclerView.ViewHolder(binding.root) {
        val notesIconDrawable: Drawable? = ContextCompat.getDrawable(binding.root.context, R.drawable.ic_note_day)
        val accentColor = ContextCompat.getColor(binding.root.context, R.color.colorAccent)
    }

    private class BlankDayViewHolder(binding: ItemClassBlankBinding) : RecyclerView.ViewHolder(binding.root)

    private class DayEndViewHolder(binding: ItemDayEndBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffUtilCallback(
        private val oldData: List<BaseItemView>,
        private val newData: List<BaseItemView>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldData.size
        }

        override fun getNewListSize(): Int {
            return newData.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItemView: BaseItemView = oldData[oldItemPosition]
            val newItemView: BaseItemView = newData[newItemPosition]
            return if (oldItemView.viewType == newItemView.viewType) {
                when (oldItemView.viewType) {
                    ItemViewType.STUDYGROUPCLASS -> {
                        oldItemView as StudyGroupClassItemView
                        newItemView as StudyGroupClassItemView
                        oldItemView.weekdayId == newItemView.weekdayId &&
                            oldItemView.classDesciptor.startTime == newItemView.classDesciptor.startTime &&
                            oldItemView.classDesciptor.endTime == newItemView.classDesciptor.endTime
                    }
                    ItemViewType.TEACHERCLASS -> {
                        oldItemView as TeacherClassItemView
                        newItemView as TeacherClassItemView
                        oldItemView.weekdayId == newItemView.weekdayId &&
                            oldItemView.classDesciptor.startTime == newItemView.classDesciptor.startTime &&
                            oldItemView.classDesciptor.endTime == newItemView.classDesciptor.endTime
                    }
                    ItemViewType.DAY -> {
                        oldItemView as DayItemView
                        newItemView as DayItemView
                        oldItemView.weekdayId == newItemView.weekdayId
                    }
                    else -> true
                }
            } else {
                false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItemView: BaseItemView = oldData[oldItemPosition]
            val newItemView: BaseItemView = newData[newItemPosition]
            return when (oldItemView.viewType) {
                ItemViewType.STUDYGROUPCLASS -> {
                    oldItemView as StudyGroupClassItemView
                    newItemView as StudyGroupClassItemView
                    oldItemView.classDesciptor.subject == newItemView.classDesciptor.subject &&
                        oldItemView.classDesciptor.type == newItemView.classDesciptor.type &&
                        oldItemView.classDesciptor.classroom == newItemView.classDesciptor.classroom &&
                        oldItemView.classDesciptor.teacher == newItemView.classDesciptor.teacher
                }
                ItemViewType.TEACHERCLASS -> {
                    oldItemView as TeacherClassItemView
                    newItemView as TeacherClassItemView
                    oldItemView.classDesciptor.subject == newItemView.classDesciptor.subject &&
                        oldItemView.classDesciptor.type == newItemView.classDesciptor.type &&
                        oldItemView.classDesciptor.classroom == newItemView.classDesciptor.classroom &&
                        oldItemView.classDesciptor.faculty == newItemView.classDesciptor.faculty &&
                        oldItemView.classDesciptor.groups == newItemView.classDesciptor.groups
                }
                ItemViewType.DAY -> {
                    oldItemView as DayItemView
                    newItemView as DayItemView
                    oldItemView.hasNotes == newItemView.hasNotes
                }
                else -> true
            }
        }
    }
}
