package by.ntnk.msluschedule.ui.adapters

import android.content.res.Resources
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Note
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.ArrayList
import java.util.Random

class NoteRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val data = ArrayList<Note>()
    private val viewColors = ArrayList<Int>()
    private val rnd = Random()
    private val onLongClickSubject = PublishSubject.create<Note>()

    val onLongClickObservable: Observable<Note>
        get() = onLongClickSubject

    fun initData(notes: List<Note>, resources: Resources) {
        data.clear()
        data.addAll(notes)
        viewColors.clear()
        data.onEach { viewColors.add(getViewColor(resources)) }
        notifyDataSetChanged()
    }

    fun addNote(note: Note, resources: Resources) {
        data.add(note)
        viewColors.add(getViewColor(resources))
        notifyItemInserted(data.size - 1)
    }

    private fun getViewColor(resources: Resources): Int {
        val colors = resources.obtainTypedArray(R.array.note_colors)
        var color = colors.getColor(rnd.nextInt(colors.length()), Color.BLACK)
        while (color in viewColors && colors.length() > viewColors.size) {
            color = colors.getColor(rnd.nextInt(colors.length()), Color.BLACK)
        }
        colors.recycle()
        return color
    }

    fun getNote(position: Int): Note = data[position]

    fun findNotePosition(note: String): Int = data.indexOfFirst { it.text == note }

    fun getColor(position: Int): Int = viewColors[position]

    fun removeAt(position: Int) {
        data.removeAt(position)
        viewColors.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size)
    }

    fun restoreItem(position: Int, note: Note, color: Int) {
        data.add(position, note)
        viewColors.add(position, color)
        notifyItemInserted(position)
    }

    fun changeItem(position: Int, note: Note) {
        data[position] = note
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        with(viewHolder as NoteViewHolder) {
            noteText.text = data[position].text
            colorStripe.setBackgroundColor(viewColors[position])

            itemView.setOnLongClickListener {
                onLongClickSubject.onNext(data[position])
                return@setOnLongClickListener true
            }
        }
    }

    override fun getItemCount(): Int = data.size

    private class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteText: TextView = view.findViewById(R.id.text_note)
        val colorStripe: View = view.findViewById(R.id.view_note_colorstripe)
    }
}
