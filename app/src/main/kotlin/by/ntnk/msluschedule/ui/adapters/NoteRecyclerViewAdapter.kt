package by.ntnk.msluschedule.ui.adapters

import android.content.res.Resources
import android.graphics.Color
import android.util.SparseBooleanArray
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
import java.util.Random

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

    private val itemViews = mutableListOf<BaseItemView>()
    private val colors = mutableListOf<Int>()
    private val sortedClasses = mutableListOf<Lesson>()
    private val sortedNotes: LinkedHashMap<String, MutableList<NoteItemView>> = LinkedHashMap()
    private val usedColors = SparseBooleanArray()
    private val onLongClickSubject = PublishSubject.create<Note>()
    private var selectedNoteItemView: NoteItemView? = null

    val onLongClickObservable: Observable<Note>
        get() = onLongClickSubject

    fun onNewData(notes: List<Note>, classes: List<Lesson>, resources: Resources) {
        initNoteColors(resources)

        sortedNotes.clear()
        sortedClasses.clear()
        usedColors.clear()

        for (classDescriptor in classes) {
            sortedClasses.add(classDescriptor)
            val subjectKey = if (classDescriptor.type != String.DEFAULT) {
                "${classDescriptor.subject}, ${classDescriptor.type}"
            } else {
                classDescriptor.subject
            }
            sortedNotes[subjectKey] = mutableListOf()
        }

        for (note in notes) {
            if (sortedNotes[note.subject] == null) {
                sortedNotes[note.subject] = mutableListOf()
            }
            sortedNotes[note.subject]!!.add(NoteItemView(note, getViewColor()))
        }

        onDataChanged()
    }

    private fun initNoteColors(resources: Resources) {
        colors.clear()
        val resColors = resources.obtainTypedArray(R.array.note_colors)
        for (i in 0 until resColors.length()) {
            colors.add(resColors.getColor(i, Color.BLUE))
        }
        resColors.recycle()
    }

    private fun onDataChanged() {
        val olditemViews = ArrayList(itemViews)
        itemViews.clear()

        for ((subject, notes) in sortedNotes) {
            if (notes.isNotEmpty()) {
                val filteredClasses = sortedClasses
                    .filter { classDescriptor ->
                        val subjectKey = if (classDescriptor.type != String.DEFAULT) {
                            "${classDescriptor.subject}, ${classDescriptor.type}"
                        } else {
                            classDescriptor.subject
                        }
                        return@filter subjectKey == subject
                    }
                var time = String.DEFAULT
                for (classDescriptor in filteredClasses) {
                    time = time.plus(classDescriptor.startTime + " / ")
                }
                if (time.endsWith(" / ")) {
                    time = time.dropLast(3)
                }
                itemViews.add(SubjectItemView(subject, time))
                itemViews.addAll(notes)
            }
        }

        val callback = DiffUtilCallback(olditemViews, itemViews)
        val diffResult = DiffUtil.calculateDiff(callback)
        diffResult.dispatchUpdatesTo(this)
    }

    private fun getViewColor(): Int {
        if (usedColors.size() >= colors.size) {
            usedColors.clear()
        }

        var idx = Random().nextInt(colors.size)
        while (usedColors[idx]) {
            idx = Random().nextInt(colors.size)
        }
        usedColors.put(idx, true)
        return colors[idx]
    }

    fun addNote(note: Note) {
        if (sortedNotes[note.subject] == null) {
            sortedNotes[note.subject] = mutableListOf()
        }

        sortedNotes[note.subject]!!.add(NoteItemView(note, getViewColor()))

        onDataChanged()
    }

    fun restoreNote(note: Note, color: Int, position: Int) {
        if (sortedNotes[note.subject] == null) {
            sortedNotes[note.subject] = mutableListOf()
        }

        sortedNotes[note.subject]!!.add(position, NoteItemView(note, color))

        onDataChanged()
    }

    fun updateSelectedNote(updatedNote: Note) {
        if (sortedNotes[updatedNote.subject] == null) {
            sortedNotes[updatedNote.subject] = mutableListOf()
        }
        val oldNoteIdx: Int = sortedNotes[selectedNoteItemView!!.note.subject]!!.indexOf(selectedNoteItemView!!)
        if (selectedNoteItemView!!.note.subject == updatedNote.subject) {
            val oldNoteItemView = sortedNotes[updatedNote.subject]!![oldNoteIdx]
            sortedNotes[updatedNote.subject]!![oldNoteIdx] = NoteItemView(updatedNote, oldNoteItemView.color)
        } else {
            sortedNotes[selectedNoteItemView!!.note.subject]!!.removeAt(oldNoteIdx)
            sortedNotes[updatedNote.subject]!!.add(NoteItemView(updatedNote, getViewColor()))
        }

        deselectNote()

        onDataChanged()
    }

    fun getSelectedNoteId(): Int? = selectedNoteItemView?.note?.id

    fun removeNote(position: Int): Triple<Note, Int, Int> {
        val noteItemView = itemViews[position] as NoteItemView
        val noteItemViewIdx = sortedNotes[noteItemView.note.subject]!!.indexOf(noteItemView)
        sortedNotes[noteItemView.note.subject]!!.remove(noteItemView)
        onDataChanged()
        return Triple(noteItemView.note, noteItemView.color, noteItemViewIdx)
    }

    fun deselectNote() {
        selectedNoteItemView = null
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
                            selectedNoteItemView = noteItemView
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
