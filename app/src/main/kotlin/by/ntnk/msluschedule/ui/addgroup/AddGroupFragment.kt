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
import by.ntnk.msluschedule.utils.Utils
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
                binding.autocompletetextGroup.text.clear()
                binding.autocompletetextGroup.setEnabledFocusable(false)
                presenter.resetGroups()

                binding.autocompletetextCourse.text.clear()
                binding.autocompletetextCourse.setEnabledFocusable(false)
                presenter.resetCourses()

                presenter.setFacultyKeyFromPosition(position)
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
                binding.autocompletetextGroup.text.clear()
                binding.autocompletetextGroup.setEnabledFocusable(false)
                presenter.resetGroups()

                presenter.setCourseKeyFromPosition(position)
            }
            setOnClickListener { view ->
                view as AutoCompleteTextView
                view.showDropDown()
            }
        }

        with(binding.autocompletetextGroup) {
            setEnabledFocusable(false)
            setOnItemClickListener { _, _, _, id ->
                presenter.setGroupKey(id.toInt())
            }
            doOnTextChanged { _, _, before, _ ->
                if (before > 0) {
                    presenter.setGroupKey(Int.DEFAULT)
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

        disposables += presenter.scheduleContanersObservable.subscribeBy(
            onError = { throwable -> showError(throwable) }
        )

        disposables += presenter.facultyFilterObservable.subscribeBy(
            onNext = { state ->
                if (state.isLoading) {
                    binding.progressindicatorFaculty.visibility = View.VISIBLE
                } else {
                    binding.progressindicatorFaculty.visibility = View.GONE
                }

                if (state.isData()) {
                    onFacultyFilterChanged(state.data)
                }

                if (state.isError()) {
                    showError(state.throwable!!)
                }
            }
        )

        disposables += presenter.facultyKeyObservable.subscribeBy(
            onNext = { key ->
                if (key != Int.DEFAULT) {
                    if (presenter.isCourseFilterUiStateDefault()) {
                        presenter.downloadCourseFilter()
                    }
                }
            }
        )

        disposables += presenter.courseFilterObservable.subscribeBy(
            onNext = { state ->
                if (state.isLoading) {
                    binding.progressindicatorCourse.visibility = View.VISIBLE
                } else {
                    binding.progressindicatorCourse.visibility = View.GONE
                }

                if (state.isData()) {
                    onCourseFilterChanged(state.data)
                }

                if (state.isError()) {
                    showError(state.throwable!!)
                }
            }
        )

        disposables += presenter.courseKeyObservable.subscribeBy(
            onNext = { key ->
                if (key != Int.DEFAULT) {
                    if (presenter.isGroupFilterUiStateDefault()) {
                        presenter.downloadGroupFilter()
                    }
                }
            }
        )

        disposables += presenter.groupFilterObservable.subscribeBy(
            onNext = { state ->
                if (state.isLoading) {
                    binding.progressindicatorGroup.visibility = View.VISIBLE
                } else {
                    binding.progressindicatorGroup.visibility = View.GONE
                }

                if (state.isData()) {
                    onGroupFilterChanged(state.data)
                }

                if (state.isError()) {
                    showError(state.throwable!!)
                }
            }
        )

        disposables += presenter.groupKeyObservable.subscribeBy(
            onNext = { key ->
                val alertDialog = dialog as AlertDialog
                var isEnabled = false
                if (key != Int.DEFAULT) {
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
        if (Utils.isNetworkException(t)) {
            Timber.i(t)
        } else {
            Timber.e(t)
        }
        dismiss()
        listener.onError(t)
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
        fun onError(t: Throwable)
    }
}
