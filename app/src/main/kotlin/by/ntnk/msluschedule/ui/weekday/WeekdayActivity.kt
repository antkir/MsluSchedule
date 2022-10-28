package by.ntnk.msluschedule.ui.weekday

import android.animation.Animator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.databinding.ActivityWeekdayBinding
import by.ntnk.msluschedule.mvp.views.MvpActivity
import by.ntnk.msluschedule.ui.adapters.NoteRecyclerViewAdapter
import by.ntnk.msluschedule.ui.adapters.VIEWTYPE_NOTE
import by.ntnk.msluschedule.ui.customviews.ItemSwipeCallback
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.INVALID_VALUE
import by.ntnk.msluschedule.utils.SimpleAnimatorListener
import by.ntnk.msluschedule.utils.SimpleTextWatcher
import by.ntnk.msluschedule.utils.dipToPixels
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipDrawable
import com.google.android.material.snackbar.Snackbar
import dagger.Lazy
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

class WeekdayActivity :
    MvpActivity<WeekdayPresenter, WeekdayView>(),
    WeekdayView,
    HasAndroidInjector {

    private var weekdayId: Int = INVALID_VALUE
    private var layoutManagerSavedState: Parcelable? = null
    private val disposables = CompositeDisposable()

    private lateinit var binding: ActivityWeekdayBinding

    private val adapter: NoteRecyclerViewAdapter
        get() = binding.recyclerviewNotes.adapter as NoteRecyclerViewAdapter? ?: NoteRecyclerViewAdapter()

    override val view: WeekdayView
        get() = this

    @Inject
    lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var injectedPresenter: Lazy<WeekdayPresenter>

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    override fun onCreatePresenter(): WeekdayPresenter = injectedPresenter.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        binding = ActivityWeekdayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        weekdayId = intent?.getIntExtra(ARG_WEEKDAY_ID, INVALID_VALUE) ?: INVALID_VALUE
        layoutManagerSavedState = savedInstanceState?.getParcelable(ARG_LAYOUT_MANAGER_SAVED_STATE)

        binding.recyclerviewNotes.apply {
            layoutManager = LinearLayoutManager(this@WeekdayActivity)
            setHasFixedSize(true)
            adapter = this@WeekdayActivity.adapter
            val swipeHandler = createSwipeHandler()
            val itemTouchHelper = ItemTouchHelper(swipeHandler)
            itemTouchHelper.attachToRecyclerView(this)
        }

        binding.fabAddNote.setOnClickListener { showNoteEditLayout() }
        binding.buttonSaveNote.setOnClickListener { onSaveNoteClick() }
        val buttonInactiveTint = ContextCompat.getColor(this@WeekdayActivity, R.color.ic_save_note_inactive_tint)
        binding.buttonSaveNote.setColorFilter(buttonInactiveTint)
        binding.edittextNote.addTextChangedListener(object : SimpleTextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val color = if (s.isBlank()) {
                    ContextCompat.getColor(this@WeekdayActivity, R.color.ic_save_note_inactive_tint)
                } else {
                    ContextCompat.getColor(this@WeekdayActivity, R.color.ic_save_note_tint)
                }
                binding.buttonSaveNote.setColorFilter(color)
            }
        })
        initOnLongClickNoteListener()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    private fun showNoteEditLayout() {
        binding.fabAddNote.visibility = View.INVISIBLE

        val editNotelayoutAnimDelay: Long = 50
        val editNoteLayoutAnimDuration: Long = 100

        val backgroundColor = ContextCompat.getColor(applicationContext, R.color.unfocused_background)
        binding.layoutEditNote.setBackgroundColor(backgroundColor)
        binding.layoutEditNote.visibility = View.VISIBLE
        binding.layoutEditNote.isFocusable = true
        binding.layoutEditNote.isClickable = true
        binding.layoutEditNote.translationY = binding.layoutEditNote.height.toFloat()
        binding.layoutEditNote.animate()
            .translationY(0f)
            .setStartDelay(editNotelayoutAnimDelay)
            .setDuration(editNoteLayoutAnimDuration)
            .setInterpolator(FastOutSlowInInterpolator())
            .setListener(object : SimpleAnimatorListener {
                val alphaAnimation = AlphaAnimation(0f, 1f).apply {
                    startOffset = editNotelayoutAnimDelay
                    duration = editNoteLayoutAnimDuration
                }

                override fun onAnimationStart(animation: Animator?) {
                    binding.layoutEditNote.startAnimation(alphaAnimation)
                }

                override fun onAnimationEnd(animation: Animator?) {
                    binding.layoutEditNote.animate()?.setListener(null)
                }
            })
            .start()

        binding.scrollViewChips.translationY = binding.scrollViewChips.height.toFloat()
        binding.scrollViewChips.visibility = View.VISIBLE
        binding.scrollViewChips.animate()
            .translationY(0f)
            .setStartDelay(editNotelayoutAnimDelay + editNoteLayoutAnimDuration)
            .setDuration(150)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()

        binding.edittextNote.requestFocus()

        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.edittextNote, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun onSaveNoteClick() {
        if (binding.edittextNote.text?.isNotBlank() == true) {
            val selectedNoteId = adapter.getSelectedNoteId()
            val subject = getCheckedChipText()
            if (selectedNoteId != null) {
                val updatedNote = Note(selectedNoteId, binding.edittextNote.text.toString(), subject)
                adapter.updateSelectedNote(updatedNote)
                presenter.updateNote(updatedNote, weekdayId)
            } else {
                presenter.insertNote(Note(0, binding.edittextNote.text.toString(), subject), weekdayId)
            }

            hideEditNoteLayout(resetViews = true)
        }
    }

    private fun hideEditNoteLayout(resetViews: Boolean, isEditNoteLayoutAnimEnabled: Boolean = false) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.edittextNote.windowToken, 0)

        val editNoteLayoutAnimDuration: Long = 100

        binding.fabAddNote.scaleX = 0f
        binding.fabAddNote.scaleY = 0f
        binding.fabAddNote.visibility = View.VISIBLE
        binding.fabAddNote.animate()
            .setDuration(150)
            .scaleY(1f)
            .scaleX(1f)
            .setStartDelay(editNoteLayoutAnimDuration)
            .setInterpolator(FastOutSlowInInterpolator())
            .start()

        val backgroundColor = ContextCompat.getColor(applicationContext, R.color.transparent)
        binding.layoutEditNote.setBackgroundColor(backgroundColor)
        binding.layoutEditNote.isClickable = false
        binding.layoutEditNote.isFocusable = false

        if (isEditNoteLayoutAnimEnabled) {
            binding.layoutEditNote.animate()
                .translationY(binding.edittextNote.height.toFloat())
                .setDuration(editNoteLayoutAnimDuration)
                .setInterpolator(FastOutSlowInInterpolator())
                .setListener(object : SimpleAnimatorListener {
                    override fun onAnimationEnd(animation: Animator?) {
                        binding.scrollViewChips.translationY = binding.scrollViewChips.height.toFloat()
                        binding.scrollViewChips.visibility = View.INVISIBLE

                        binding.layoutEditNote.visibility = View.INVISIBLE
                        binding.layoutEditNote.animate()?.setListener(null)

                        if (resetViews || adapter.getSelectedNoteId() != null) {
                            resetEditNoteLayoutViews()
                        }
                    }
                })
                .start()
        } else {
            binding.scrollViewChips.translationY = binding.scrollViewChips.height.toFloat()
            binding.scrollViewChips.visibility = View.INVISIBLE

            binding.layoutEditNote.visibility = View.INVISIBLE

            if (resetViews || adapter.getSelectedNoteId() != null) {
                resetEditNoteLayoutViews()
            }
        }
    }

    private fun resetEditNoteLayoutViews() {
        adapter.deselectNote()
        binding.edittextNote.text?.clear()
        binding.chipsSubjects.clearCheck()
        binding.scrollViewChips.scrollTo(0, 0)
    }

    private fun getCheckedChipText(): String {
        for (i in 0 until binding.chipsSubjects.childCount) {
            val chip = binding.chipsSubjects.getChildAt(i) as Chip
            if (chip.id == binding.chipsSubjects.checkedChipId) {
                return chip.text.toString()
            }
        }
        return EMPTY_STRING
    }

    private fun createSwipeHandler(): ItemSwipeCallback {
        return object : ItemSwipeCallback(binding.recyclerviewNotes.context) {
            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                return if (viewHolder.itemViewType == VIEWTYPE_NOTE && adapter.getSelectedNoteId() == null) {
                    super.getSwipeDirs(recyclerView, viewHolder)
                } else {
                    NO_DIRECION
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val (note, noteViewColor, noteSubgroupPosition) = adapter.removeNote(viewHolder.adapterPosition)
                animateZeroNotesView()
                presenter.deleteNote(note.id)

                val noteDeleted = resources.getString(R.string.snackbar_note_deleted)
                val snackbar = Snackbar.make(binding.coordinatorLayout, noteDeleted, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.fabAddNote)
                    .setAction(resources.getString(R.string.snackbar_action_undo)) {
                        presenter.restoreNote(note, weekdayId, noteViewColor, noteSubgroupPosition)
                    }
                ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
                snackbar.show()
            }
        }
    }

    private fun animateZeroNotesView() {
        if (adapter.itemCount == 0) {
            val alphaAnimation = AlphaAnimation(0f, 1f).apply {
                startOffset = 100
                duration = 100
            }
            binding.textZeroNotes.startAnimation(alphaAnimation)
            binding.textZeroNotes.visibility = View.VISIBLE
        }
    }

    private fun initOnLongClickNoteListener() {
        disposables += adapter.onLongClickObservable.subscribeBy(
            onNext = { note ->
                for (i in 0 until binding.chipsSubjects.childCount) {
                    val chip = binding.chipsSubjects.getChildAt(i) as Chip
                    chip.isChecked = chip.text == note.subject
                }
                binding.edittextNote.setText(note.text, TextView.BufferType.EDITABLE)
                showNoteEditLayout()
            },
            onError = { throwable -> Timber.e(throwable) }
        )
    }

    override fun onStart() {
        super.onStart()
        if (weekdayId <= 0) {
            NavUtils.navigateUpFromSameTask(this)
            return
        }

        binding.coordinatorLayout.viewTreeObserver.addOnGlobalLayoutListener(onKeyboardStateChangeListener)
        presenter.initWeekdayView(weekdayId)
    }

    override fun onStop() {
        super.onStop()
        if (adapter.getSelectedNoteId() != null) {
            hideEditNoteLayout(resetViews = true)
        }
        binding.coordinatorLayout.viewTreeObserver.removeOnGlobalLayoutListener(onKeyboardStateChangeListener)
    }

    private val onKeyboardStateChangeListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        private var isKeyboardVisible = false

        override fun onGlobalLayout() {
            val heightDiff = binding.coordinatorLayout.rootView.height - binding.coordinatorLayout.height
            val wasKeyboardVisible = isKeyboardVisible
            val isKeyboardVisible = heightDiff > dipToPixels(200f)
            if (!isKeyboardVisible && wasKeyboardVisible) {
                hideEditNoteLayout(resetViews = false)
            }
            this.isKeyboardVisible = isKeyboardVisible
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(ARG_LAYOUT_MANAGER_SAVED_STATE, binding.recyclerviewNotes.layoutManager?.onSaveInstanceState())
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    override fun onBackPressed() {
        if (binding.layoutEditNote.visibility == View.VISIBLE) {
            hideEditNoteLayout(resetViews = true, isEditNoteLayoutAnimEnabled = true)
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
        supportActionBar?.title = AndroidUtils.getWeekdayFromTag(weekdayWithLessons.weekday, applicationContext)

        adapter.initData(data, weekdayWithLessons.lessons, resources)
        if (adapter.itemCount > 0) {
            binding.textZeroNotes.visibility = View.GONE
        }
        binding.recyclerviewNotes.layoutManager?.onRestoreInstanceState(layoutManagerSavedState)

        if (binding.chipsSubjects.childCount == 0) {
            val lessonSubjects = weekdayWithLessons.lessons
                .map { if (it.type != EMPTY_STRING) "${it.subject}, ${it.type}" else it.subject }
                .toSet()
            if (lessonSubjects.isEmpty()) {
                binding.chipsSubjects.visibility = View.GONE
            }

            for (lessonSubject in lessonSubjects) {
                if (lessonSubject == EMPTY_STRING) continue

                val chip = Chip(this)
                val chipDrawable = ChipDrawable.createFromAttributes(this, null, 0, R.style.MsluTheme_Chip_Filter)
                chip.setChipDrawable(chipDrawable)
                chip.setTextAppearanceResource(R.style.MsluTheme_Chip_Text_Appearance)
                chip.text = lessonSubject
                binding.chipsSubjects.addView(chip)
            }
        }
    }

    override fun addNote(note: Note) {
        binding.textZeroNotes.visibility = View.GONE
        adapter.addNote(note)
    }

    override fun restoreNote(note: Note, color: Int, position: Int) {
        binding.textZeroNotes.visibility = View.GONE
        adapter.restoreNote(note, color, position)
    }

    companion object {
        private const val ARG_WEEKDAY_ID = "weekdayId"
        private const val ARG_LAYOUT_MANAGER_SAVED_STATE = "argLayoutManagerSavedState"

        fun startActivity(context: Context, weekdayId: Int) {
            val intent = Intent(context, WeekdayActivity::class.java).apply {
                putExtra(ARG_WEEKDAY_ID, weekdayId)
            }
            context.startActivity(intent)
        }
    }
}
