package by.ntnk.msluschedule.ui.adapters

import android.content.res.Resources
import android.graphics.Color
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.utils.BaseRVItemView
import by.ntnk.msluschedule.utils.EMPTY_STRING
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.Random

const val VIEWTYPE_NOTE = 201
const val VIEWTYPE_NOTE_HEADER = 202

class NoteRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val data = mutableListOf<BaseRVItemView>()
    private val colors = mutableListOf<Int>()
    private val sortedLesssons = mutableListOf<Lesson>()
    private val sortedNotes: LinkedHashMap<String, MutableList<NoteView>> = LinkedHashMap()
    private val usedColors = SparseBooleanArray()
    private val onLongClickSubject = PublishSubject.create<Note>()
    private var selectedNote: NoteView? = null
    private lateinit var otherNotesHeader: String

    val onLongClickObservable: Observable<Note>
        get() = onLongClickSubject

    fun initData(notes: List<Note>, lessons: List<Lesson>, resources: Resources) {
        initNoteColors(resources)
        otherNotesHeader = resources.getString(R.string.textview_other_notes_header)

        sortedNotes.clear()
        sortedLesssons.clear()
        usedColors.clear()

        for (lesson in lessons) {
            sortedLesssons.add(lesson)
            val subjectKey = if (lesson.type != EMPTY_STRING) "${lesson.subject}, ${lesson.type}" else lesson.subject
            sortedNotes[subjectKey] = mutableListOf()
        }

        for (note in notes) {
            if (sortedNotes[note.subject] == null) {
                sortedNotes[note.subject] = mutableListOf()
            }
            sortedNotes[note.subject]!!.add(NoteView(note, getViewColor()))
        }

        updateData()
    }

    private fun initNoteColors(resources: Resources) {
        colors.clear()
        val resColors = resources.obtainTypedArray(R.array.note_colors)
        for (i in 0 until resColors.length()) {
            colors.add(resColors.getColor(i, Color.BLUE))
        }
        resColors.recycle()
    }

    private fun updateData() {
        val oldData: List<BaseRVItemView> = ArrayList(data)
        data.clear()
        for ((subject, notes) in sortedNotes) {
            if (notes.isNotEmpty()) {
                val header = if (subject == EMPTY_STRING) otherNotesHeader else subject
                val filteredLessons = sortedLesssons
                    .filter {
                        val subjectKey = if (it.type != EMPTY_STRING) "${it.subject}, ${it.type}" else it.subject
                        return@filter subjectKey == header
                    }
                var time = EMPTY_STRING
                for (lesson in filteredLessons) {
                    time = time.plus(lesson.startTime + " / ")
                }
                if (time.endsWith(" / ")) {
                    time = time.dropLast(3)
                }
                data.add(NoteHeaderView(header, time))
                data.addAll(notes)
            }
        }

        val diffResult = DiffUtil.calculateDiff(DiffUtilCallback(oldData, data))
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

        sortedNotes[note.subject]!!.add(NoteView(note, getViewColor()))

        updateData()
    }

    fun restoreNote(note: Note, color: Int, position: Int) {
        if (sortedNotes[note.subject] == null) {
            sortedNotes[note.subject] = mutableListOf()
        }

        sortedNotes[note.subject]!!.add(position, NoteView(note, color))

        updateData()
    }

    fun updateSelectedNote(updatedNote: Note) {
        if (sortedNotes[updatedNote.subject] == null) {
            sortedNotes[updatedNote.subject] = mutableListOf()
        }
        val oldNoteIdx: Int = sortedNotes[selectedNote!!.note.subject]!!.indexOf(selectedNote!!)
        if (selectedNote!!.note.subject == updatedNote.subject) {
            val oldNoteView = sortedNotes[updatedNote.subject]!![oldNoteIdx]
            sortedNotes[updatedNote.subject]!![oldNoteIdx] = NoteView(updatedNote, oldNoteView.color)
        } else {
            sortedNotes[selectedNote!!.note.subject]!!.removeAt(oldNoteIdx)
            sortedNotes[updatedNote.subject]!!.add(NoteView(updatedNote, getViewColor()))
        }

        deselectNote()

        updateData()
    }

    fun getSelectedNoteId(): Int? = selectedNote?.note?.id

    fun removeNote(position: Int): Triple<Note, Int, Int> {
        val noteView = data[position] as NoteView
        val noteViewIdx = sortedNotes[noteView.note.subject]!!.indexOf(noteView)
        sortedNotes[noteView.note.subject]!!.remove(noteView)
        updateData()
        return Triple(noteView.note, noteView.color, noteViewIdx)
    }

    fun deselectNote() {
        selectedNote = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEWTYPE_NOTE_HEADER -> {
                val view = inflater.inflate(R.layout.item_note_header, parent, false)
                NoteHeaderViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_note, parent, false)
                NoteViewHolder(view).apply {
                    itemView.setOnLongClickListener {
                        if (adapterPosition != NO_POSITION) {
                            val noteView = data[adapterPosition] as NoteView
                            selectedNote = noteView
                            onLongClickSubject.onNext(noteView.note)
                        }
                        return@setOnLongClickListener true
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        data[position].bindViewHolder(viewHolder)
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int = data[position].viewType

    class NoteHeaderView(val header: String, private val startTime: String?) : BaseRVItemView {
        override val viewType: Int = VIEWTYPE_NOTE_HEADER

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as NoteHeaderViewHolder) {
                subject.text = header
                if (startTime != null) {
                    time.text = startTime
                }
            }
        }
    }

    class NoteView(val note: Note, val color: Int) : BaseRVItemView {
        override val viewType: Int = VIEWTYPE_NOTE

        override fun bindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            with(viewHolder as NoteViewHolder) {
                noteText.text = note.text
                colorStripe.setBackgroundColor(color)
            }
        }
    }

    private class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteText: TextView = view.findViewById(R.id.text_note)
        val colorStripe: View = view.findViewById(R.id.view_note_colorstripe)
    }

    private class NoteHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val subject: TextView = view.findViewById(R.id.text_note_subject)
        val time: TextView = view.findViewById(R.id.text_note_time)
    }

    private class DiffUtilCallback(
        private val oldData: List<BaseRVItemView>,
        private val newData: List<BaseRVItemView>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldData.size

        override fun getNewListSize(): Int = newData.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldData[oldItemPosition]
            val newItem = newData[newItemPosition]
            val isSameNoteView = oldItem is NoteView && newItem is NoteView && oldItem.note.id == newItem.note.id
            val isSameNoteHeaderView = oldItem is NoteHeaderView && newItem is NoteHeaderView && oldItem.header == newItem.header
            return isSameNoteView || isSameNoteHeaderView
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldData[oldItemPosition]
            val newItem = newData[newItemPosition]
            val isSameContentNoteView = oldItem is NoteView &&
                newItem is NoteView &&
                oldItem.note.id == newItem.note.id &&
                oldItem.note.text == newItem.note.text &&
                oldItem.note.subject == newItem.note.subject
            val isSameContentNoteHeaderView = oldItem is NoteHeaderView && newItem is NoteHeaderView && oldItem.header == newItem.header
            return isSameContentNoteView || isSameContentNoteHeaderView
        }
    }
}
