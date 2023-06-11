package by.ntnk.msluschedule.ui.weekday

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isNotEmpty
import androidx.core.widget.doOnTextChanged
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Lesson
import by.ntnk.msluschedule.data.Note
import by.ntnk.msluschedule.data.WeekdayWithLessons
import by.ntnk.msluschedule.databinding.ActivityWeekdayBinding
import by.ntnk.msluschedule.mvp.views.MvpActivity
import by.ntnk.msluschedule.ui.adapters.NoteRecyclerViewAdapter
import by.ntnk.msluschedule.ui.customviews.ItemSwipeCallback
import by.ntnk.msluschedule.utils.AndroidUtils
import by.ntnk.msluschedule.utils.DEFAULT
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
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

class WeekdayActivity :
    MvpActivity<WeekdayPresenter, WeekdayView>(),
    WeekdayView,
    HasAndroidInjector {

    private var weekdayId: Int = Int.DEFAULT
    private lateinit var adapterOnClickDisposable: Disposable
    private val disposables: CompositeDisposable = CompositeDisposable()

    private lateinit var binding: ActivityWeekdayBinding

    override val view: WeekdayView
        get() = this

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            presenter.destroy()
            remove()
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private val onEditNoteBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            hideEditNoteLayout(resetViews = true, isEditNoteLayoutAnimEnabled = true)
        }
    }

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

        weekdayId = intent?.getIntExtra(ARG_WEEKDAY_ID, Int.DEFAULT) ?: Int.DEFAULT

        binding.recyclerviewNotes.apply {
            layoutManager = LinearLayoutManager(this@WeekdayActivity)
            setHasFixedSize(true)
            adapter = NoteRecyclerViewAdapter()
            val swipeHandler = createSwipeHandler()
            val itemTouchHelper = ItemTouchHelper(swipeHandler)
            itemTouchHelper.attachToRecyclerView(this)
        }

        val adapter = binding.recyclerviewNotes.adapter as NoteRecyclerViewAdapter
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        adapter.setupNoteColors(resources)
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (itemCount == 1 && positionStart != NO_POSITION) {
                    binding.recyclerviewNotes.smoothScrollToPosition(positionStart)
                }
            }
        })

        binding.fabAddNote.setOnClickListener { showEditNoteLayout() }
        binding.buttonSaveNote.setOnClickListener { onSaveNoteClick() }
        val buttonInactiveTint = ContextCompat.getColor(this@WeekdayActivity, R.color.ic_save_note_inactive_tint)
        binding.buttonSaveNote.setColorFilter(buttonInactiveTint)
        binding.edittextNote.doOnTextChanged { text, _, _, _ ->
            val color = if (text?.isNotBlank() == true) {
                ContextCompat.getColor(this@WeekdayActivity, R.color.ic_save_note_tint)
            } else {
                ContextCompat.getColor(this@WeekdayActivity, R.color.ic_save_note_inactive_tint)
            }
            binding.buttonSaveNote.setColorFilter(color)
        }

        observeAdapterOnLongClick()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightMode == Configuration.UI_MODE_NIGHT_NO) {
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                insetsController.isAppearanceLightNavigationBars = true
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        onBackPressedDispatcher.addCallback(this, onEditNoteBackPressedCallback)
    }

    private fun showEditNoteLayout() {
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
            .setListener(object : AnimatorListenerAdapter() {
                private val alphaAnimation = AlphaAnimation(0f, 1f).apply {
                    startOffset = editNotelayoutAnimDelay
                    duration = editNoteLayoutAnimDuration
                }

                override fun onAnimationStart(animation: Animator) {
                    binding.layoutEditNote.startAnimation(alphaAnimation)
                }

                override fun onAnimationEnd(animation: Animator) {
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

        onEditNoteBackPressedCallback.isEnabled = true
    }

    private fun onSaveNoteClick() {
        if (binding.edittextNote.text?.isNotBlank() == true) {
            val adapter = binding.recyclerviewNotes.adapter as NoteRecyclerViewAdapter
            val text = binding.edittextNote.text.toString()
            val subject = getCheckedChipText()
            if (adapter.isNoteSelected()) {
                val selectedNoteId = adapter.getSelectedNoteId()
                val note = Note(selectedNoteId, text, subject)
                presenter.updateNote(note, weekdayId)
            } else {
                val note = Note(0, text, subject)
                presenter.insertNote(note, weekdayId)
            }

            hideEditNoteLayout(resetViews = true)
        }
    }

    private fun hideEditNoteLayout(resetViews: Boolean, isEditNoteLayoutAnimEnabled: Boolean = false) {
        val adapter = binding.recyclerviewNotes.adapter as NoteRecyclerViewAdapter

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
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        binding.layoutEditNote.animate()?.setListener(null)
                        binding.layoutEditNote.visibility = View.INVISIBLE

                        binding.scrollViewChips.translationY = binding.scrollViewChips.height.toFloat()
                        binding.scrollViewChips.visibility = View.INVISIBLE

                        if (resetViews || adapter.isNoteSelected()) {
                            resetEditNoteLayoutViews()
                        }
                    }
                })
                .start()
        } else {
            binding.layoutEditNote.visibility = View.INVISIBLE

            binding.scrollViewChips.translationY = binding.scrollViewChips.height.toFloat()
            binding.scrollViewChips.visibility = View.INVISIBLE

            if (resetViews || adapter.isNoteSelected()) {
                resetEditNoteLayoutViews()
            }
        }

        onEditNoteBackPressedCallback.isEnabled = false
    }

    private fun resetEditNoteLayoutViews() {
        val adapter = binding.recyclerviewNotes.adapter as NoteRecyclerViewAdapter
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
        return String.DEFAULT
    }

    private fun createSwipeHandler(): ItemSwipeCallback {
        return object : ItemSwipeCallback(binding.recyclerviewNotes.context) {
            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val adapter = binding.recyclerviewNotes.adapter as NoteRecyclerViewAdapter
                return if (viewHolder.itemViewType == NoteRecyclerViewAdapter.ItemViewType.NOTE.toInt() && !adapter.isNoteSelected()) {
                    super.getSwipeDirs(recyclerView, viewHolder)
                } else {
                    NO_DIRECION
                }
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.recyclerviewNotes.adapter as NoteRecyclerViewAdapter
                val note = adapter.getNoteFromPosition(viewHolder.bindingAdapterPosition)
                presenter.deleteNote(note.id)

                val noteDeleted = resources.getString(R.string.snackbar_note_deleted)
                val snackbar = Snackbar.make(binding.coordinatorLayout, noteDeleted, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.fabAddNote)
                    .setAction(resources.getString(R.string.snackbar_action_undo)) {
                        presenter.insertNote(note, weekdayId)
                    }
                ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
                snackbar.show()
            }
        }
    }

    private fun showZeroNotesView() {
        val alphaAnimation = AlphaAnimation(0f, 1f).apply {
            startOffset = 100
            duration = 100
        }
        binding.textZeroNotes.startAnimation(alphaAnimation)
        binding.textZeroNotes.visibility = View.VISIBLE
    }

    private fun observeAdapterOnLongClick() {
        val adapter = binding.recyclerviewNotes.adapter as NoteRecyclerViewAdapter
        adapterOnClickDisposable = adapter.onLongClickObservable.subscribeBy(
            onNext = { note ->
                for (i in 0 until binding.chipsSubjects.childCount) {
                    val chip = binding.chipsSubjects.getChildAt(i) as Chip
                    chip.isChecked = chip.text == note.subject
                }
                binding.edittextNote.setText(note.text, TextView.BufferType.EDITABLE)
                binding.edittextNote.setSelection(note.text.length)
                showEditNoteLayout()
            },
            onError = { throwable -> Timber.e(throwable) }
        )
    }

    override fun onStart() {
        super.onStart()

        if (weekdayId <= 0) {
            Timber.e(IllegalArgumentException())
            presenter.destroy()
            NavUtils.navigateUpFromSameTask(this)
            return
        }

        binding.coordinatorLayout.viewTreeObserver.addOnGlobalLayoutListener(onKeyboardStateChangeListener)

        disposables += presenter.weekdayUiStateObservable
            .subscribeBy(
                onNext = { weekdayUiState ->
                    if (weekdayUiState == WeekdayUiState()) {
                        presenter.getWeekday(weekdayId)
                    }

                    if (weekdayUiState.isData()) {
                        val weekday = weekdayUiState.data!!
                        onWeekdayData(weekday)

                        if (presenter.isNotesUiStateDefault()) {
                            presenter.observeNotes(weekday)
                        }
                    }

                    if (weekdayUiState.isError()) {
                        val throwable = weekdayUiState.throwable!!
                        Timber.e(throwable)
                    }
                }
            )

        disposables += presenter.notesUiStateObservable
            .subscribeBy(
                onNext = { notesUiState ->
                    if (notesUiState.isData()) {
                        val notes = notesUiState.data!!
                        val weekday = presenter.getWeekdayData()
                        onNotesData(notes, weekday.classes)
                    }

                    if (notesUiState.isError()) {
                        val throwable = notesUiState.throwable!!
                        Timber.e(throwable)
                    }
                }
            )
    }

    override fun onStop() {
        super.onStop()
        val adapter = binding.recyclerviewNotes.adapter as NoteRecyclerViewAdapter
        if (adapter.isNoteSelected()) {
            hideEditNoteLayout(resetViews = true)
        }
        binding.coordinatorLayout.viewTreeObserver.removeOnGlobalLayoutListener(onKeyboardStateChangeListener)
        disposables.clear()
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

    override fun onDestroy() {
        super.onDestroy()
        adapterOnClickDisposable.dispose()
        binding.layoutEditNote.animate().setListener(null)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            presenter.destroy()
            NavUtils.navigateUpFromSameTask(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onWeekdayData(weekday: WeekdayWithLessons<Lesson>) {
        supportActionBar?.title = AndroidUtils.getWeekdayFromTag(weekday.value, applicationContext)

        if (binding.chipsSubjects.isNotEmpty()) {
            return
        }

        val subjects = weekday.classes
            .map { classDescriptor ->
                if (classDescriptor.type != String.DEFAULT) {
                    "${classDescriptor.subject}, ${classDescriptor.type}"
                } else {
                    classDescriptor.subject
                }
            }
            .toSet()

        if (subjects.isEmpty()) {
            binding.chipsSubjects.visibility = View.GONE
        } else {
            for (subject in subjects) {
                if (subject == String.DEFAULT) {
                    continue
                }

                val chipDrawable = ChipDrawable.createFromAttributes(this, null, 0, R.style.MsluTheme_Chip_Filter)
                val chip = Chip(this).apply {
                    setChipDrawable(chipDrawable)
                    setTextAppearanceResource(R.style.MsluTheme_Chip_Text_Appearance)
                    text = subject
                }
                binding.chipsSubjects.addView(chip)
            }
        }
    }

    private fun onNotesData(notes: List<Note>, classes: List<Lesson>) {
        val adapter = binding.recyclerviewNotes.adapter as NoteRecyclerViewAdapter
        adapter.onNewData(notes, classes)
        if (adapter.isEmpty()) {
            showZeroNotesView()
        } else {
            binding.textZeroNotes.visibility = View.GONE
        }
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
