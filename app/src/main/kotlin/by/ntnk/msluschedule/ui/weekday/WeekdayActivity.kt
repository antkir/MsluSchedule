package by.ntnk.msluschedule.ui.weekday

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.mvp.views.MvpActivity
import by.ntnk.msluschedule.ui.adapters.NoteRecyclerViewAdapter
import by.ntnk.msluschedule.ui.customviews.ItemSwipeCallback
import by.ntnk.msluschedule.utils.INVALID_VALUE
import by.ntnk.msluschedule.utils.SimpleAnimatorListener
import by.ntnk.msluschedule.utils.dipToPixels
import by.ntnk.msluschedule.utils.getWeekdayFromTag
import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_weekday.*
import javax.inject.Inject

class WeekdayActivity : MvpActivity<WeekdayPresenter, WeekdayView>(),
        WeekdayView,
        HasSupportFragmentInjector {
    private lateinit var recyclerView: RecyclerView
    private var weekdayId: Int = INVALID_VALUE
    private var updatedNoteIndex: Int = INVALID_VALUE
    private var keyboardIsShown = false

    override val view: WeekdayView
        get() = this

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var injectedPresenter: Lazy<WeekdayPresenter>

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentDispatchingAndroidInjector

    override fun onCreatePresenter(): WeekdayPresenter = injectedPresenter.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekday)
        setSupportActionBar(toolbar_weekday)

        weekdayId = intent?.getIntExtra(ARG_WEEKDAY_ID, INVALID_VALUE) ?: INVALID_VALUE

        fab_weekday.setOnClickListener { onNoteChangeFabClick() }

        button_save_note.setOnClickListener { onSaveNoteClick() }

        recyclerView = findViewById(R.id.recyclerview_weekday)
        with(recyclerView) {
            layoutManager = LinearLayoutManager(this@WeekdayActivity)
            setHasFixedSize(true)
            adapter = NoteRecyclerViewAdapter()
            val swipeHandler = createSwipeHandler()
            val itemTouchHelper = ItemTouchHelper(swipeHandler)
            itemTouchHelper.attachToRecyclerView(recyclerView)
        }

        initOnLongClickNoteListener()
        initOnKeyboardStateChangeListener()

        presenter.getWeekday(weekdayId)
        presenter.getNotes(weekdayId)
    }

    private fun onNoteChangeFabClick() {
        fab_weekday.visibility = View.INVISIBLE
        edittext_note.visibility = View.VISIBLE
        edittext_note.translationY = edittext_note.height.toFloat()
        edittext_note.animate()
                .translationY(0f)
                .setStartDelay(100)
                .setDuration(100)
                .start()

        button_save_note.visibility = View.VISIBLE
        button_save_note.translationY = button_save_note.height.toFloat()
        button_save_note.animate()
                .translationY(0f)
                .setStartDelay(100)
                .setDuration(100)
                .start()

        edittext_note_shadow?.visibility = View.VISIBLE

        edittext_note.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(edittext_note, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun onSaveNoteClick() {
        if (edittext_note.text.isNotBlank()) {
            val adapter = recyclerView.adapter as NoteRecyclerViewAdapter
            if (updatedNoteIndex >= 0) {
                val noteId = adapter.getNote(updatedNoteIndex).id
                val updatedNote = Note(noteId, edittext_note.text.toString())
                adapter.changeItem(updatedNoteIndex, updatedNote)
                presenter.updateNote(updatedNote, weekdayId)
            } else {
                val isDuplicate = adapter.findNotePosition(edittext_note.text.toString()) != INVALID_VALUE
                if (!isDuplicate) {
                    presenter.insertNote(edittext_note.text.toString(), weekdayId)
                }
            }

            hideEditNoteView()
        }
    }

    private fun createSwipeHandler(): ItemSwipeCallback {
        return object : ItemSwipeCallback(recyclerView.context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
                val adapter = recyclerView.adapter as NoteRecyclerViewAdapter
                val deletedNoteIndex = viewHolder!!.adapterPosition
                val note: Note = adapter.getNote(deletedNoteIndex)
                val color: Int = adapter.getColor(deletedNoteIndex)
                adapter.removeAt(deletedNoteIndex)
                if (adapter.itemCount == 0) {
                    textview_zeronotes.visibility = View.VISIBLE
                }
                val noteDeleted = resources.getString(R.string.snackbar_note_deleted)
                Snackbar.make(constraintlayout_weekday, noteDeleted, Snackbar.LENGTH_LONG)
                        .setAction(resources.getString(R.string.snackbar_action_undo)) {
                            adapter.restoreItem(deletedNoteIndex, note, color)
                            textview_zeronotes.visibility = View.GONE
                        }
                        .addCallback(object : Snackbar.Callback() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                if (event != DISMISS_EVENT_ACTION && !isChangingConfigurations) {
                                    presenter.deleteNote(note.id)
                                }
                            }
                        })
                        .show()
            }
        }
    }

    private fun initOnLongClickNoteListener() {
        val adapter = recyclerView.adapter as NoteRecyclerViewAdapter
        adapter.onLongClickObservable.subscribeBy(
                onNext = {
                    edittext_note.setText(it.text, TextView.BufferType.EDITABLE)
                    fab_weekday.performClick()
                    updatedNoteIndex = adapter.findNotePosition(it.text)
                },
                onError = { it.printStackTrace() }
        )
    }

    private fun initOnKeyboardStateChangeListener() {
        val activityRootView: View = findViewById(R.id.constraintlayout_weekday)
        activityRootView.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = activityRootView.rootView.height - activityRootView.height
            if (heightDiff > applicationContext.dipToPixels(200f)) {
                keyboardIsShown = true
            } else {
                if (keyboardIsShown) {
                    hideEditNoteView()
                }
                keyboardIsShown = false
                updatedNoteIndex = INVALID_VALUE
            }
        }
    }

    private fun hideEditNoteView() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(edittext_note.windowToken, 0)

        edittext_note.text.clear()
        edittext_note.visibility = View.INVISIBLE
        button_save_note.visibility = View.INVISIBLE
        edittext_note_shadow?.visibility = View.GONE

        fab_weekday.animate()
                .setListener(object : SimpleAnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        fab_weekday.animate().setListener(null)
                        fab_weekday.visibility = View.VISIBLE
                    }
                })
                .start()
    }

    override fun onBackPressed() {
        if (edittext_note.visibility == View.VISIBLE) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(edittext_note.windowToken, 0)

            button_save_note.visibility = View.INVISIBLE
            edittext_note_shadow?.visibility = View.GONE
            edittext_note.animate()
                    .translationY(edittext_note.height.toFloat())
                    .setDuration(100)
                    .setListener(object : SimpleAnimatorListener {
                        override fun onAnimationEnd(animation: Animator?) {
                            edittext_note.animate().setListener(null)
                            edittext_note.visibility = View.INVISIBLE
                            edittext_note.translationY = 0f
                            edittext_note.text.clear()

                            fab_weekday.visibility = View.VISIBLE
                        }
                    })
                    .start()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun setToolbar(weekday: String) {
        toolbar_weekday.title = getWeekdayFromTag(weekday, applicationContext)
    }

    override fun showNotes(data: List<Note>) {
        val adapter = recyclerView.adapter as NoteRecyclerViewAdapter
        adapter.initData(data, resources)
        if (adapter.itemCount > 0) {
            textview_zeronotes.visibility = View.GONE
        }
    }

    override fun addNote(note: Note) {
        val adapter = recyclerView.adapter as NoteRecyclerViewAdapter
        adapter.addNote(note, resources)
    }

    companion object {
        private const val ARG_WEEKDAY_ID = "weekdayId"

        fun startActivity(context: Context, weekdayId: Int) {
            val intent = Intent(context.applicationContext, WeekdayActivity::class.java).apply {
                putExtra(ARG_WEEKDAY_ID, weekdayId)
            }
            context.startActivity(intent)
        }
    }
}