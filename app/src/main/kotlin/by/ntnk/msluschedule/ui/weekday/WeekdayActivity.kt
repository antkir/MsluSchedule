package by.ntnk.msluschedule.ui.weekday

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.mvp.views.MvpActivity
import by.ntnk.msluschedule.ui.adapters.NoteRecyclerViewAdapter
import by.ntnk.msluschedule.ui.customviews.ItemSwipeCallback
import by.ntnk.msluschedule.utils.*
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.activity_weekday.*
import timber.log.Timber
import javax.inject.Inject

private const val ARG_LAYOUT_MANAGER_SAVED_STATE = "argLayoutManagerSavedState"

class WeekdayActivity : MvpActivity<WeekdayPresenter, WeekdayView>(),
        WeekdayView,
        HasSupportFragmentInjector {
    private lateinit var recyclerView: RecyclerView
    private var weekdayId: Int = INVALID_VALUE
    private var updatedNoteIndex: Int = INVALID_VALUE
    private var keyboardIsShown = false
    private var layoutManagerSavedState: Parcelable? = null
    private val disposables = CompositeDisposable()

    override val view: WeekdayView
        get() = this

    @Inject
    lateinit var fragmentDispatchingAndroidInjector: DispatchingAndroidInjector<Fragment>

    @Inject
    lateinit var injectedPresenter: Lazy<WeekdayPresenter>

    @Inject
    lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentDispatchingAndroidInjector

    override fun onCreatePresenter(): WeekdayPresenter = injectedPresenter.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        val themeMode = sharedPreferencesRepository.getThemeMode().toInt()
        AppCompatDelegate.setDefaultNightMode(themeMode)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekday)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        weekdayId = intent?.getIntExtra(ARG_WEEKDAY_ID, INVALID_VALUE) ?: INVALID_VALUE
        layoutManagerSavedState = savedInstanceState?.getParcelable(ARG_LAYOUT_MANAGER_SAVED_STATE)

        fab_weekday.setOnClickListener { showNoteEditLayout() }

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
    }

    private fun showNoteEditLayout() {
        (fab_weekday as View).visibility = View.INVISIBLE

        layout_edit_note.visibility = View.VISIBLE
        layout_edit_note.translationY = edittext_note.height.toFloat()
        layout_edit_note.animate()
                .translationY(0f)
                .setStartDelay(50)
                .setDuration(100)
                .setListener(object : SimpleAnimatorListener {
                    val alphaAnimation = AlphaAnimation(0f, 1f).apply {
                        startOffset = 50
                        duration = 100
                    }
                    val backgroundColor = ContextCompat.getColor(applicationContext, R.color.unfocused_background)

                    override fun onAnimationStart(animation: Animator?) {
                        layout_edit_note?.startAnimation(alphaAnimation)
                        layout_edit_note?.isFocusable = true
                        layout_edit_note?.isClickable = true
                        layout_edit_note?.setBackgroundColor(backgroundColor)
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        layout_edit_note?.animate()?.setListener(null)
                    }
                })
                .start()

        edittext_note.requestFocus()
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(edittext_note, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun onSaveNoteClick() {
        if (edittext_note.text?.isNotBlank() == true) {
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

            hideEditNoteLayout()
            edittext_note.text?.clear()
        }
    }

    private fun createSwipeHandler(): ItemSwipeCallback {
        return object : ItemSwipeCallback(recyclerView.context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Sometimes update layout for a note can be triggered by user after the note
                // was removed which leads to crash if we don't set updatedNoteIndex to INVALID_VALUE.
                updatedNoteIndex = INVALID_VALUE

                val adapter = recyclerView.adapter as NoteRecyclerViewAdapter
                val deletedNoteIndex = viewHolder.adapterPosition
                val note: Note = adapter.getNote(deletedNoteIndex)
                val color: Int = adapter.getColor(deletedNoteIndex)
                val noteDeleted = resources.getString(R.string.snackbar_note_deleted)

                adapter.removeAt(deletedNoteIndex)
                if (adapter.itemCount == 0) {
                    textview_zeronotes.visibility = View.VISIBLE
                }
                presenter.deleteNote(note.id)

                Snackbar.make(constraintlayout_weekday, noteDeleted, Snackbar.LENGTH_LONG)
                        .setAction(resources.getString(R.string.snackbar_action_undo)) {
                            presenter.restoreNote(note.text, deletedNoteIndex, color, weekdayId)
                            textview_zeronotes.visibility = View.GONE
                        }
                        .show()
            }
        }
    }

    private fun initOnLongClickNoteListener() {
        val adapter = recyclerView.adapter as NoteRecyclerViewAdapter
        disposables += adapter.onLongClickObservable.subscribeBy(
                onNext = {
                    updatedNoteIndex = adapter.findNotePosition(it.text)
                    edittext_note.setText(it.text, TextView.BufferType.EDITABLE)
                    showNoteEditLayout()
                },
                onError = { throwable -> Timber.e(throwable) }
        )
    }

    private fun hideEditNoteLayout() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(edittext_note.windowToken, 0)

        layout_edit_note.isClickable = false
        layout_edit_note.isFocusable = false
        layout_edit_note.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.transparent))
        layout_edit_note.visibility = View.INVISIBLE

        (fab_weekday as View).visibility = View.VISIBLE
        fab_weekday.scaleX = 0f
        fab_weekday.scaleY = 0f
        fab_weekday.animate()
                .setDuration(300)
                .scaleY(1f)
                .scaleX(1f)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
    }

    override fun onStart() {
        super.onStart()
        if (weekdayId <= 0) {
            NavUtils.navigateUpFromSameTask(this)
            return
        }
        presenter.getWeekday(weekdayId)
        presenter.getNotes(weekdayId)

        constraintlayout_weekday.viewTreeObserver.addOnGlobalLayoutListener(onKeyboardStateChangeListener)
    }

    override fun onStop() {
        super.onStop()
        constraintlayout_weekday.viewTreeObserver.removeOnGlobalLayoutListener(onKeyboardStateChangeListener)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_LAYOUT_MANAGER_SAVED_STATE, recyclerView.layoutManager?.onSaveInstanceState())
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    private val onKeyboardStateChangeListener = ViewTreeObserver.OnGlobalLayoutListener {
        val heightDiff = constraintlayout_weekday.rootView.height - constraintlayout_weekday.height
        if (heightDiff > applicationContext.dipToPixels(200f)) {
            keyboardIsShown = true
        } else {
            if (keyboardIsShown) {
                hideEditNoteLayout()
                updatedNoteIndex = INVALID_VALUE
            }
            keyboardIsShown = false
        }
    }

    override fun onBackPressed() {
        if (layout_edit_note.visibility == View.VISIBLE) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(edittext_note.windowToken, 0)

            layout_edit_note.isClickable = false
            layout_edit_note.isFocusable = false
            val backgroundColor = ContextCompat.getColor(applicationContext, R.color.transparent)
            layout_edit_note.setBackgroundColor(backgroundColor)
            layout_edit_note.animate()
                    .translationY(edittext_note.height.toFloat())
                    .setDuration(100)
                    .setListener(object : SimpleAnimatorListener {
                        override fun onAnimationEnd(animation: Animator?) {
                            layout_edit_note?.animate()?.setListener(null)
                            layout_edit_note?.visibility = View.INVISIBLE
                            layout_edit_note?.translationY = 0f
                            edittext_note?.text?.clear()

                            (fab_weekday as View?)?.visibility = View.VISIBLE
                        }
                    })
                    .start()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun setToolbar(weekday: String) {
        supportActionBar?.title = getWeekdayFromTag(weekday, applicationContext)
    }

    override fun showNotes(data: List<Note>) {
        val adapter = recyclerView.adapter as NoteRecyclerViewAdapter
        adapter.initData(data, resources)
        if (adapter.itemCount > 0) {
            textview_zeronotes.visibility = View.GONE
        }
        recyclerView.layoutManager?.onRestoreInstanceState(layoutManagerSavedState)
    }

    override fun addNote(note: Note) {
        val adapter = recyclerView.adapter as NoteRecyclerViewAdapter
        adapter.addNote(note, resources)
    }

    override fun restoreNote(note: Note, position: Int, color: Int) {
        val adapter = recyclerView.adapter as NoteRecyclerViewAdapter
        adapter.restoreItem(position, note, color)
    }

    companion object {
        private const val ARG_WEEKDAY_ID = "weekdayId"

        fun startActivity(context: Context, weekdayId: Int) {
            val intent = Intent(context, WeekdayActivity::class.java).apply {
                putExtra(ARG_WEEKDAY_ID, weekdayId)
            }
            context.startActivity(intent)
        }
    }
}
