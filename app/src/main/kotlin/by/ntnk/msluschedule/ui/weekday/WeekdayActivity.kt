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
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.mvp.views.MvpActivity
import by.ntnk.msluschedule.ui.adapters.NoteRecyclerViewAdapter
import by.ntnk.msluschedule.ui.adapters.VIEWTYPE_NOTE
import by.ntnk.msluschedule.ui.customviews.ItemSwipeCallback
import by.ntnk.msluschedule.utils.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
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
    private var keyboardIsShown = false
    private var layoutManagerSavedState: Parcelable? = null
    private val disposables = CompositeDisposable()

    private val adapter: NoteRecyclerViewAdapter
        get() = if (recyclerView.adapter == null) {
            NoteRecyclerViewAdapter()
        } else {
            recyclerView.adapter as NoteRecyclerViewAdapter
        }

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

        recyclerView = findViewById(R.id.recyclerview_weekday)
        with(recyclerView) {
            layoutManager = LinearLayoutManager(this@WeekdayActivity)
            setHasFixedSize(true)
            adapter = this@WeekdayActivity.adapter
            val swipeHandler = createSwipeHandler()
            val itemTouchHelper = ItemTouchHelper(swipeHandler)
            itemTouchHelper.attachToRecyclerView(recyclerView)
        }

        fab_weekday.setOnClickListener { showNoteEditLayout() }
        button_save_note.setOnClickListener { onSaveNoteClick() }
        val buttonInactiveTint = ContextCompat.getColor(this@WeekdayActivity, R.color.ic_save_note_inactive_tint)
        button_save_note.setColorFilter(buttonInactiveTint)
        edittext_note.addTextChangedListener(object : SimpleTextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val color = if (s.isBlank()) {
                    ContextCompat.getColor(this@WeekdayActivity, R.color.ic_save_note_inactive_tint)
                } else {
                    ContextCompat.getColor(this@WeekdayActivity, R.color.ic_save_note_tint)
                }
                button_save_note.setColorFilter(color)
            }
        })
        initOnLongClickNoteListener()
    }

    private fun showNoteEditLayout() {
        (fab_weekday as View).visibility = View.INVISIBLE

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
                        layout_edit_note?.visibility = View.VISIBLE
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
            val selectedNoteId = adapter.getSelectedNoteId()
            val subject = getCheckedChipText()
            if (selectedNoteId != null) {
                val updatedNote = Note(selectedNoteId, edittext_note.text.toString(), subject)
                adapter.updateSelectedNote(updatedNote)
                presenter.updateNote(updatedNote, weekdayId)
                adapter.deselectNote()
            } else {
                presenter.insertNote(Note(0, edittext_note.text.toString(), subject), weekdayId)
            }

            hideEditNoteLayout()
            edittext_note.text?.clear()
            chips_subjects.clearCheck()
            scroll_view_chips.scrollTo(0, 0)
        }
    }

    private fun getCheckedChipText(): String {
        var subject = EMPTY_STRING
        for (i in 0 until chips_subjects.childCount) {
            val chip = chips_subjects.getChildAt(i) as Chip
            if (chip.id == chips_subjects.checkedChipId) {
                subject = chip.text.toString()
            }
        }
        return subject
    }

    private fun createSwipeHandler(): ItemSwipeCallback {
        return object : ItemSwipeCallback(recyclerView.context) {
            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return if (viewHolder.itemViewType == VIEWTYPE_NOTE && adapter.getSelectedNoteId() == null) {
                    super.getSwipeDirs(recyclerView, viewHolder)
                } else {
                    NO_DIRECION
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val noteView = adapter.getNoteView(viewHolder.adapterPosition)
                val noteMapIndex = adapter.getMapIndexOfNote(noteView)
                adapter.removeNote(noteView)
                if (adapter.itemCount == 0) {
                    val alphaAnimation = AlphaAnimation(0f, 1f).apply {
                        startOffset = 200
                        duration = 100
                    }
                    textview_zeronotes.startAnimation(alphaAnimation)
                    textview_zeronotes.visibility = View.VISIBLE
                }

                val noteDeleted = resources.getString(R.string.snackbar_note_deleted)
                Snackbar.make(constraintlayout_weekday, noteDeleted, Snackbar.LENGTH_LONG)
                        .addCallback(object : Snackbar.Callback() {
                            override fun onDismissed(snackbar: Snackbar, event: Int) {
                                if (event != DISMISS_EVENT_ACTION) {
                                    presenter.deleteNote(noteView.note.id)
                                }
                            }
                        })
                        .setAction(resources.getString(R.string.snackbar_action_undo)) {
                            adapter.restoreNote(noteMapIndex, noteView)
                            textview_zeronotes.visibility = View.GONE
                        }
                        .show()
            }
        }
    }

    private fun initOnLongClickNoteListener() {
        disposables += adapter.onLongClickObservable.subscribeBy(
                onNext = { note ->
                    for (i in 0 until chips_subjects.childCount) {
                        val chip = chips_subjects.getChildAt(i) as Chip
                        if (chip.text == note.subject) {
                            chip.isChecked = true
                        }
                    }
                    edittext_note.setText(note.text, TextView.BufferType.EDITABLE)
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

        presenter.initWeekdayView(weekdayId)
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

    private val onKeyboardStateChangeListener = ViewTreeObserver.OnGlobalLayoutListener {
        val heightDiff = constraintlayout_weekday.rootView.height - constraintlayout_weekday.height
        keyboardIsShown =
                if (heightDiff > applicationContext.dipToPixels(200f)) {
                    true
                } else {
                    if (keyboardIsShown) {
                        hideEditNoteLayout()
                        if (adapter.getSelectedNoteId() != null) {
                            adapter.deselectNote()
                            edittext_note.text?.clear()
                            chips_subjects.clearCheck()
                            scroll_view_chips.scrollTo(0, 0)
                        }
                    }
                    false
                }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
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

    override fun initView(weekdayWithLessons: WeekdayWithLessons<Lesson>, data: List<Note>) {
        supportActionBar?.title = getWeekdayFromTag(weekdayWithLessons.weekday, applicationContext)

        adapter.initData(data, weekdayWithLessons.lessons, resources)
        if (adapter.itemCount > 0) {
            textview_zeronotes.visibility = View.GONE
        }
        recyclerView.layoutManager?.onRestoreInstanceState(layoutManagerSavedState)

        if (chips_subjects.childCount == 0) {
            val lessonSubjects = weekdayWithLessons.lessons.map { it.subject }.toSet()
            if (lessonSubjects.isEmpty()) {
                chips_subjects.visibility = View.GONE
            }

            for (lessonSubject in lessonSubjects) {
                if (lessonSubject == EMPTY_STRING) continue

                val chip = Chip(this)
                val chipDrawable = ChipDrawable.createFromAttributes(this, null, 0, R.style.MsluTheme_Chip_Choice)
                chip.setChipDrawable(chipDrawable)
                chip.text = lessonSubject
                chips_subjects.addView(chip)
            }
        }
    }

    override fun addNote(note: Note) {
        textview_zeronotes.visibility = View.GONE
        adapter.addNote(note)
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
