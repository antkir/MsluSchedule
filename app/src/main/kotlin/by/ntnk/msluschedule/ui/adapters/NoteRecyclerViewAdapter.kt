package by.ntnk.msluschedule.ui.adapters

import android.content.res.Resources
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.databinding.ItemNoteBinding
import by.ntnk.msluschedule.databinding.ItemSubjectBinding
import by.ntnk.msluschedule.utils.DEFAULT
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlin.math.absoluteValue

class NoteRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class ItemViewType {
        NOTE,
        SUBJECT;

        fun toInt(): Int = ordinal
    }

    interface BaseItemView {
        val viewType: ItemViewType
        fun bindViewHolder(viewHolder: RecyclerView.ViewHolder)
    }

    private val colors = mutableListOf<Int>()
    private val itemViews = mutableListOf<BaseItemView>()
    private val onLongClickSubject = PublishSubject.create<Note>()
    private var selectedNoteId: Int = Int.DEFAULT

    val onLongClickObservable: Observable<Note>
        get() = onLongClickSubject

    fun initializeNoteColors(resources: Resources) {
        colors.clear()
        val resColors = resources.obtainTypedArray(R.array.note_colors)
        for (i in 0 until resColors.length()) {
            colors.add(resColors.getColor(i, Color.BLUE))
        }
        resColors.recycle()
    }

    fun onNewData(notes: List<Note>, classes: List<Lesson>) {
        val olditemViews = ArrayList(itemViews)
        itemViews.clear()

        val sortedData = linkedMapOf<String, MutableList<Note>>()
        for (classDescriptor in classes) {
            val subject = getClassSubjectText(classDescriptor)
            if (subject != String.DEFAULT) {
                sortedData[subject] = mutableListOf()
            }
        }
        for (note in notes) {
            val subject = note.subject
            if (subject != String.DEFAULT) {
                if (!sortedData.contains(subject)) {
                    sortedData[subject] = mutableListOf()
                }
                sortedData[subject]!!.add(note)
            }
        }

        // Notes with default subject should always be at the end of the list.
        sortedData[String.DEFAULT] = mutableListOf()
        for (note in notes) {
            if (note.subject == String.DEFAULT) {
                sortedData[String.DEFAULT]!!.add(note)
            }
        }

        for ((subject, sortedNotes) in sortedData) {
            if (sortedNotes.isNotEmpty()) {
                val time = getClassTimeText(subject, classes)
                itemViews.add(SubjectItemView(subject, time))
                for (note in sortedNotes) {
                    itemViews.add(NoteItemView(note, getNoteColor(note)))
                }
            }
        }

        val callback = DiffUtilCallback(olditemViews, itemViews)
        val diffResult = DiffUtil.calculateDiff(callback)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun getClassSubjectText(classDescriptor: Lesson): String {
        return if (classDescriptor.type != String.DEFAULT) {
            "${classDescriptor.subject}, ${classDescriptor.type}"
        } else {
            classDescriptor.subject
        }
    }

    private fun getClassTimeText(subject: String, classes: List<Lesson>): String {
        val filteredClasses = classes.filter { classDescriptor -> getClassSubjectText(classDescriptor) == subject }
        var time = String.DEFAULT
        for ((index, classDescriptor) in filteredClasses.withIndex()) {
            time += classDescriptor.startTime
            if (index < filteredClasses.size - 1) {
                time += " / "
            }
        }
        return time
    }

    private fun getNoteColor(note: Note): Int {
        val hashCodeAbs = note.text.hashCode().absoluteValue
        val index = (if (hashCodeAbs != Int.MIN_VALUE) hashCodeAbs else Int.MAX_VALUE) % colors.size
        return colors[index]
    }

    fun getSelectedNoteId(): Int {
        return selectedNoteId
    }

    fun isNoteSelected(): Boolean {
        return selectedNoteId != Int.DEFAULT
    }

    fun deselectNote() {
        selectedNoteId = Int.DEFAULT
    }

    fun getNoteFromPosition(position: Int): Note {
        val noteItemView = itemViews[position] as NoteItemView
        return noteItemView.note
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemViewType.SUBJECT.toInt() -> {
                val binding = ItemSubjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                SubjectViewHolder(binding)
            }
            else -> {
                val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                NoteViewHolder(binding).apply {
                    itemView.setOnLongClickListener {
                        if (adapterPosition != NO_POSITION) {
                            val noteItemView = itemViews[adapterPosition] as NoteItemView
                            selectedNoteId = noteItemView.note.id
                            onLongClickSubject.onNext(noteItemView.note)
                        }
                        return@setOnLongClickListener true
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        itemViews[position].bindViewHolder(viewHolder)
    }

    override fun getItemCount(): Int = itemViews.size

    override fun getItemViewType(position: Int): Int = itemViews[position].viewType.toInt()

    fun isEmpty(): Boolean = itemViews.size == 0

    class SubjectItemView(val subject: String, private val startTime: String?) : BaseItemView {
        override val viewType: ItemViewType = ItemViewType.SUBJECT

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as SubjectViewHolder) {
                if (subject != String.DEFAULT) {
                    binding.textSubject.text = subject
                } else {
                    binding.textSubject.text =
                        binding.textSubject.context.getString(R.string.textview_other_notes_header)
                }
                if (startTime != null) {
                    binding.textTime.text = startTime
                }
            }
        }
    }

    class NoteItemView(val note: Note, val color: Int) : BaseItemView {
        override val viewType: ItemViewType = ItemViewType.NOTE

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as NoteViewHolder) {
                binding.textNote.text = note.text
                binding.viewStripeSide.setBackgroundColor(color)
            }
        }
    }

    private class NoteViewHolder(val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root)

    private class SubjectViewHolder(val binding: ItemSubjectBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffUtilCallback(
        private val oldData: List<BaseItemView>,
        private val newData: List<BaseItemView>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldData.size

        override fun getNewListSize(): Int = newData.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItemView = oldData[oldItemPosition]
            val newItemView = newData[newItemPosition]
            return if (oldItemView.viewType == newItemView.viewType) {
                when (oldItemView.viewType) {
                    ItemViewType.SUBJECT -> {
                        oldItemView as SubjectItemView
                        newItemView as SubjectItemView
                        oldItemView.subject == newItemView.subject
                    }
                    ItemViewType.NOTE -> {
                        oldItemView as NoteItemView
                        newItemView as NoteItemView
                        oldItemView.note.id == newItemView.note.id
                    }
                }
            } else {
                false
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItemView = oldData[oldItemPosition]
            val newItemView = newData[newItemPosition]
            return when (oldItemView.viewType) {
                ItemViewType.NOTE -> {
                    oldItemView as NoteItemView
                    newItemView as NoteItemView
                    oldItemView.note.text == newItemView.note.text &&
                        oldItemView.note.subject == newItemView.note.subject
                }
                else -> true
            }
        }
    }
}
