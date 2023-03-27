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
import by.ntnk.msluschedule.ui.customviews.LoadingAutoCompleteTextView
import by.ntnk.msluschedule.utils.EMPTY_STRING
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddGroupFragment : MvpDialogFragment<AddGroupPresenter, AddGroupView>(), AddGroupView {

    private var fragmentBinding: FragmentAddGroupBinding? = null
    private val binding get() = fragmentBinding!!
    private lateinit var listener: DialogListener

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
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.setOnShowListener { dialogInterface ->
            dialogInterface as AlertDialog
            val isGroupValid = presenter.isValidGroup(binding.autocompletetextGroup.text.toString())
            dialogInterface.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isGroupValid
        }
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
                presenter.setFacultyKeyFromPosition(position)

                binding.progressindicatorCourse.visibility = View.VISIBLE
                binding.autocompletetextCourse.text.clear()
                binding.autocompletetextCourse.setEnabledFocusable(false)
                presenter.setCoursesNull()

                binding.progressindicatorGroup.visibility = View.GONE
                binding.autocompletetextGroup.text.clear()
                binding.autocompletetextGroup.setEnabledFocusable(false)
                presenter.setGroupsNull()

                presenter.getCourseScheduleFilter()
            }
            setOnClickListener { view ->
                view as LoadingAutoCompleteTextView
                view.showDropDown()
            }
        }

        with(binding.autocompletetextCourse) {
            keyListener = null
            setEnabledFocusable(false)
            setOnItemClickListener { _, _, position, _ ->
                presenter.clearDisposables()
                presenter.setCourseKeyFromPosition(position)

                binding.progressindicatorGroup.visibility = View.VISIBLE
                binding.autocompletetextGroup.text.clear()
                binding.autocompletetextGroup.setEnabledFocusable(false)
                presenter.setGroupsNull()

                presenter.getStudyGroupScheduleFilter()
            }
            setOnClickListener { view ->
                view as LoadingAutoCompleteTextView
                view.showDropDown()
            }
        }

        with(binding.autocompletetextGroup) {
            setEnabledFocusable(false)
            setOnItemClickListener { _, _, _, id ->
                presenter.setGroupId(id.toInt())
            }
            doOnTextChanged { text, _, _, _ ->
                var isEnabled = presenter.isValidGroup(text.toString())
                if (presenter.isGroupStored(text.toString())) {
                    binding.textinputlayoutGroup.error = resources.getString(R.string.group_already_added)
                    isEnabled = false
                } else {
                    binding.textinputlayoutGroup.error = EMPTY_STRING
                }
                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
            }
            setOnClickListener { view ->
                view as LoadingAutoCompleteTextView
                view.showDropDown()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (presenter.isFacultiesInitialized()) {
            presenter.populateFacultiesAdapter()

            if (presenter.isFacultySet()) {
                if (presenter.isCoursesInitialized()) {
                    presenter.populateCoursesAdapter()

                    if (presenter.isCourseSet()) {
                        if (presenter.isGroupsInitialized()) {
                            presenter.populateGroupsAdapter()
                        } else {
                            binding.progressindicatorGroup.visibility = View.VISIBLE
                            presenter.getStudyGroupScheduleFilter()
                        }
                    }
                } else {
                    binding.progressindicatorCourse.visibility = View.VISIBLE
                    presenter.getCourseScheduleFilter()
                }
            }
        } else {
            binding.progressindicatorFaculty.visibility = View.VISIBLE
            presenter.getFacultyScheduleFilter()
        }
    }

    override fun showError(t: Throwable) {
        dismiss()
        listener.onError(t)
    }

    override fun populateFacultiesAdapter(data: ScheduleFilter) {
        binding.progressindicatorFaculty.visibility = View.GONE
        val adapter = createAdapter(data, isFilteringEnabled = false)
        with(binding.autocompletetextFaculty) {
            setEnabledFocusable(true)
            setAdapter(adapter)
            requestFocus()
        }
    }

    override fun populateCoursesAdapter(data: ScheduleFilter) {
        binding.progressindicatorCourse.visibility = View.GONE
        val adapter = createAdapter(data, isFilteringEnabled = false)
        with(binding.autocompletetextCourse) {
            setEnabledFocusable(true)
            setAdapter(adapter)
            requestFocus()
        }
    }

    override fun populateGroupsAdapter(data: ScheduleFilter) {
        binding.progressindicatorGroup.visibility = View.GONE
        val adapter = createAdapter(data, isFilteringEnabled = true)
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
