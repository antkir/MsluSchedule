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
import by.ntnk.msluschedule.utils.ErrorType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import javax.inject.Inject

class AddGroupFragment : MvpDialogFragment<AddGroupPresenter, AddGroupView>(), AddGroupView {

    private var fragmentBinding: FragmentAddGroupBinding? = null
    private val binding get() = fragmentBinding!!
    private lateinit var listener: DialogListener
    private val disposables: CompositeDisposable = CompositeDisposable()

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
            setOnItemClickListener { parent, _, position, _ ->
                binding.autocompletetextGroup.text.clear()
                binding.autocompletetextGroup.setEnabledFocusable(false)
                presenter.resetGroups()

                binding.autocompletetextCourse.text.clear()
                binding.autocompletetextCourse.setEnabledFocusable(false)
                presenter.resetCourses()

                val adapter = parent.adapter as ScheduleFilterAdapter
                val key = adapter.getKey(position)
                presenter.setFacultyKey(key)
            }
            setOnClickListener { view ->
                view as AutoCompleteTextView
                view.showDropDown()
            }
        }

        with(binding.autocompletetextCourse) {
            keyListener = null
            setEnabledFocusable(false)
            setOnItemClickListener { parent, _, position, _ ->
                binding.autocompletetextGroup.text.clear()
                binding.autocompletetextGroup.setEnabledFocusable(false)
                presenter.resetGroups()

                val adapter = parent.adapter as ScheduleFilterAdapter
                val key = adapter.getKey(position)
                presenter.setCourseKey(key)
            }
            setOnClickListener { view ->
                view as AutoCompleteTextView
                view.showDropDown()
            }
        }

        with(binding.autocompletetextGroup) {
            setEnabledFocusable(false)
            setOnItemClickListener { parent, _, position, _ ->
                val adapter = parent.adapter as ScheduleFilterAdapter
                val key = adapter.getKey(position)
                presenter.setGroupKey(key)
            }
            doOnTextChanged { _, _, before, _ ->
                if (before > 0) {
                    presenter.setGroupKey(String.DEFAULT)
                }
            }
            setOnClickListener { view ->
                view as AutoCompleteTextView
                if (presenter.isGroupKeyDefault()) {
                    view.showDropDown()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        disposables += presenter.uiStateObservable
            .map { it.facultyFilterUiState.isLoading }
            .distinctUntilChanged()
            .subscribe { isLoading ->
                if (isLoading) {
                    binding.progressindicatorFaculty.visibility = View.VISIBLE
                } else {
                    binding.progressindicatorFaculty.visibility = View.GONE
                }
            }

        disposables += presenter.uiStateObservable
            .map { it.facultyFilterUiState.dataState }
            .distinctUntilChanged()
            .subscribe { dataState ->
                if (dataState.data != null) {
                    onFacultyFilterChanged(dataState.data)
                }
            }

        disposables += presenter.uiStateObservable
            .map { it.courseFilterUiState.isLoading }
            .distinctUntilChanged()
            .subscribe { isLoading ->
                if (isLoading) {
                    binding.progressindicatorCourse.visibility = View.VISIBLE
                } else {
                    binding.progressindicatorCourse.visibility = View.GONE
                }
            }

        disposables += presenter.uiStateObservable
            .map { it.courseFilterUiState.dataState }
            .distinctUntilChanged()
            .subscribe { dataState ->
                if (dataState.data != null) {
                    onCourseFilterChanged(dataState.data)
                }
            }

        disposables += presenter.uiStateObservable
            .map { it.groupFilterUiState.isLoading }
            .distinctUntilChanged()
            .subscribe { isLoading ->
                if (isLoading) {
                    binding.progressindicatorGroup.visibility = View.VISIBLE
                } else {
                    binding.progressindicatorGroup.visibility = View.GONE
                }
            }

        disposables += presenter.uiStateObservable
            .map { it.groupFilterUiState.dataState }
            .distinctUntilChanged()
            .subscribe { dataState ->
                if (dataState.data != null) {
                    onGroupFilterChanged(dataState.data)
                }
            }

        disposables += presenter.uiStateObservable
            .map { it.groupFilterUiState.key }
            .distinctUntilChanged()
            .subscribe { key ->
                val alertDialog = dialog as AlertDialog
                var isEnabled = false
                if (key != String.DEFAULT) {
                    if (presenter.isGroupAdded(key)) {
                        binding.textinputlayoutGroup.error = resources.getString(R.string.group_already_added)
                        isEnabled = false
                    } else {
                        binding.textinputlayoutGroup.isErrorEnabled = false
                        isEnabled = true
                    }
                } else {
                    binding.textinputlayoutGroup.isErrorEnabled = false
                }
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
            }

        disposables += presenter.uiStateObservable
            .map { it.storedGroupsLoadedEvent }
            .distinctUntilChanged()
            .subscribe { storedGroupsLoadedEvent ->
                if (storedGroupsLoadedEvent.isTriggered) {
                    binding.autocompletetextFaculty.text.clear()
                    binding.autocompletetextCourse.text.clear()
                    binding.autocompletetextGroup.text.clear()
                    presenter.storedGroupsLoadedEventConsumed()
                }
            }

        disposables += presenter.uiStateObservable
            .map { it.errorEvent }
            .distinctUntilChanged()
            .subscribe { errorEvent ->
                if (errorEvent.error != null) {
                    listener.showError(errorEvent.error)
                    presenter.errorEventConsumed()

                    dismiss()
                }
            }
    }

    override fun onStop() {
        super.onStop()
        disposables.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentBinding = null
    }

    private fun onFacultyFilterChanged(data: ScheduleFilter) {
        val adapter = createAdapter(data, isFilteringEnabled = false)
        with(binding.autocompletetextFaculty) {
            setEnabledFocusable(true)
            setAdapter(adapter)
            requestFocus()
        }
    }

    private fun onCourseFilterChanged(data: ScheduleFilter) {
        val adapter = createAdapter(data, isFilteringEnabled = false)
        with(binding.autocompletetextCourse) {
            setEnabledFocusable(true)
            setAdapter(adapter)
            requestFocus()
        }
    }

    private fun onGroupFilterChanged(data: ScheduleFilter) {
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

    interface DialogListener {
        fun onNewStudyGroup(studyGroup: StudyGroup)
        fun showError(error: ErrorType)
    }

    companion object {
        const val FRAGMENT_TAG = "AddGroupFragment"
    }
}
