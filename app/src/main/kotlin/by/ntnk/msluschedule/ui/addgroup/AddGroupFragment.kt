package by.ntnk.msluschedule.ui.addgroup

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.databinding.FragmentAddGroupBinding
import by.ntnk.msluschedule.mvp.views.MvpDialogFragment
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.ui.adapters.ScheduleFilterAdapter
import by.ntnk.msluschedule.ui.customviews.AutoCompleteTextView
import by.ntnk.msluschedule.utils.DEFAULT
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import javax.inject.Inject

class AddGroupFragment : MvpDialogFragment<AddGroupPresenter, AddGroupView>(), AddGroupView {

    private var fragmentBinding: FragmentAddGroupBinding? = null
    private val binding get() = fragmentBinding!!
    private lateinit var listener: DialogListener
    private val disposables: CompositeDisposable = CompositeDisposable()

    override val view: AddGroupView
        get() = this

    @Inject
    lateinit var injectedPresenter: Lazy<AddGroupPresenter>

    override fun onCreatePresenter(): AddGroupPresenter = injectedPresenter.get()

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        try {
            listener = context as DialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement DialogListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        fragmentBinding = FragmentAddGroupBinding.inflate(LayoutInflater.from(context))
        setupViews()
        val dialog = createDialog()
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        return dialog
    }

    private fun createDialog(): AlertDialog {
        return MaterialAlertDialogBuilder(requireActivity(), R.style.MsluTheme_Dialog_Alert)
            .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.rounded_corners_rect))
            .setTitle((R.string.add_group_title))
            .setView(binding.root)
            .setPositiveButton(R.string.button_add) { _, _ ->
                listener.onNewStudyGroup(presenter.createSelectedStudyGroupObject())
            }
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                dismiss()
            }
            .create()
    }

    private fun setupViews() {
        with(binding.autocompletetextFaculty) {
            keyListener = null
            setEnabledFocusable(false)
            setOnItemClickListener { _, _, position, _ ->
                presenter.clearDisposables()

                binding.progressindicatorCourse.visibility = View.VISIBLE
                binding.autocompletetextCourse.text.clear()
                binding.autocompletetextCourse.setEnabledFocusable(false)
                presenter.resetCourses()

                binding.progressindicatorGroup.visibility = View.GONE
                binding.autocompletetextGroup.text.clear()
                binding.autocompletetextGroup.setEnabledFocusable(false)
                presenter.resetGroups()

                presenter.setFacultyIdFromPosition(position)
            }
            setOnClickListener { view ->
                view as AutoCompleteTextView
                view.showDropDown()
            }
        }

        with(binding.autocompletetextCourse) {
            keyListener = null
            setEnabledFocusable(false)
            setOnItemClickListener { _, _, position, _ ->
                presenter.clearDisposables()

                binding.progressindicatorGroup.visibility = View.VISIBLE
                binding.autocompletetextGroup.text.clear()
                binding.autocompletetextGroup.setEnabledFocusable(false)
                presenter.resetGroups()

                presenter.setCourseIdFromPosition(position)
            }
            setOnClickListener { view ->
                view as AutoCompleteTextView
                view.showDropDown()
            }
        }

        with(binding.autocompletetextGroup) {
            setEnabledFocusable(false)
            setOnItemClickListener { _, _, _, id ->
                presenter.setGroupId(id.toInt())
            }
            doOnTextChanged { _, _, before, _ ->
                if (before > 0) {
                    presenter.setGroupId(Int.DEFAULT)
                }
            }
            setOnClickListener { view ->
                view as AutoCompleteTextView
                if (presenter.isGroupIdDefault()) {
                    view.showDropDown()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (presenter.isFacultyFilterDefault()) {
            binding.progressindicatorFaculty.visibility = View.VISIBLE
            presenter.getFacultyScheduleFilter()
        }

        disposables += presenter.facultyFilterObservable.subscribeBy(
            onNext = { scheduleFilter ->
                if (scheduleFilter != ScheduleFilter.DEFAULT) {
                    populateFacultiesView(scheduleFilter)
                }
            },
            onError = { throwable ->
                Timber.i(throwable)
                showError(throwable)
            }
        )

        disposables += presenter.facultyIdObservable.subscribeBy(
            onNext = { id ->
                if (id != Int.DEFAULT) {
                    if (presenter.isCourseFilterDefault()) {
                        binding.progressindicatorCourse.visibility = View.VISIBLE
                        presenter.getCourseScheduleFilter()
                    }
                }
            }
        )

        disposables += presenter.courseFilterObservable.subscribeBy(
            onNext = { scheduleFilter ->
                if (scheduleFilter != ScheduleFilter.DEFAULT) {
                    if (!presenter.isFacultyIdDefault()) {
                        populateCoursesView(scheduleFilter)
                    }
                }
            },
            onError = { throwable ->
                Timber.i(throwable)
                showError(throwable)
            }
        )

        disposables += presenter.courseIdObservable.subscribeBy(
            onNext = { id ->
                if (id != Int.DEFAULT) {
                    if (presenter.isGroupFilterDefault()) {
                        binding.progressindicatorGroup.visibility = View.VISIBLE
                        presenter.getGroupScheduleFilter()
                    }
                }
            }
        )

        disposables += presenter.groupFilterObservable.subscribeBy(
            onNext = { scheduleFilter ->
                if (scheduleFilter != ScheduleFilter.DEFAULT) {
                    if (!presenter.isFacultyIdDefault() && !presenter.isCourseIdDefault()) {
                        populateGroupsView(scheduleFilter)
                    }
                }
            },
            onError = { throwable ->
                Timber.i(throwable)
                showError(throwable)
            }
        )

        disposables += presenter.groupIdObservable.subscribeBy(
            onNext = { id ->
                val alertDialog = dialog as AlertDialog
                var isEnabled = false
                if (id != Int.DEFAULT) {
                    if (presenter.isGroupAdded(id)) {
                        binding.textinputlayoutGroup.error = resources.getString(R.string.group_already_added)
                        isEnabled = false
                    } else {
                        isEnabled = true
                    }
                } else {
                    binding.textinputlayoutGroup.isErrorEnabled = false
                }
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
            }
        )
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
    }

    private fun showError(t: Throwable) {
        dismiss()
        listener.onError(t)
    }

    private fun populateFacultiesView(data: ScheduleFilter) {
        binding.progressindicatorFaculty.visibility = View.GONE
        val adapter = createAdapter(data, isFilteringEnabled = false)
        with(binding.autocompletetextFaculty) {
            setEnabledFocusable(true)
            setAdapter(adapter)
            requestFocus()
        }
    }

    private fun populateCoursesView(data: ScheduleFilter) {
        binding.progressindicatorCourse.visibility = View.GONE
        val adapter = createAdapter(data, isFilteringEnabled = false)
        with(binding.autocompletetextCourse) {
            setEnabledFocusable(true)
            setAdapter(adapter)
            requestFocus()
        }
    }

    private fun populateGroupsView(data: ScheduleFilter) {
        binding.progressindicatorGroup.visibility = View.GONE
        val adapter = createAdapter(data, isFilteringEnabled = true)
        adapter.filter.filter(binding.autocompletetextGroup.text)
        with(binding.autocompletetextGroup) {
            inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setTypeface(null, Typeface.NORMAL)
            setEnabledFocusable(true)
            setAdapter(adapter)
            requestFocus()
        }
    }

    private fun createAdapter(data: ScheduleFilter, isFilteringEnabled: Boolean): ScheduleFilterAdapter {
        return ScheduleFilterAdapter(
            requireActivity(),
            android.R.layout.simple_spinner_dropdown_item,
            data,
            isFilteringEnabled
        )
    }

    override fun dismiss() {
        super.dismiss()
        presenter.clearDisposables()
    }

    interface DialogListener {
        fun onNewStudyGroup(studyGroup: StudyGroup)
        fun onError(t: Throwable)
    }
}
