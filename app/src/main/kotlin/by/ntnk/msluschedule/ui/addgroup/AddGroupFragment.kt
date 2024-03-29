package by.ntnk.msluschedule.ui.addgroup

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.mvp.views.MvpDialogFragment
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.ui.adapters.ScheduleFilterAdapter
import by.ntnk.msluschedule.ui.customviews.LoadingAutoCompleteTextView
import by.ntnk.msluschedule.utils.EMPTY_STRING
import by.ntnk.msluschedule.utils.SimpleTextWatcher
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddGroupFragment : MvpDialogFragment<AddGroupPresenter, AddGroupView>(), AddGroupView {
    private lateinit var listener: DialogListener
    private lateinit var facultyView: LoadingAutoCompleteTextView
    private lateinit var courseView: LoadingAutoCompleteTextView
    private lateinit var textinputlayoutCourseView: TextInputLayout
    private lateinit var groupView: LoadingAutoCompleteTextView

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
        val layout = View.inflate(activity, R.layout.add_group_view, null)
        initViews(layout)

        val dialog = initMaterialDialog(layout)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.setOnShowListener {
            (it as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    presenter.isValidGroup(groupView.text.toString())
        }
        return dialog
    }

    private fun initMaterialDialog(layout: View): Dialog {
        return MaterialAlertDialogBuilder(requireActivity(), R.style.MsluTheme_Dialog_Alert)
                .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.rounded_corners_rect))
                .setTitle((R.string.add_group_title))
                .setView(layout)
                .setPositiveButton(R.string.button_add) { _, _ ->
                    val studyGroup = presenter.getStudyGroup()
                    studyGroup ?: return@setPositiveButton
                    listener.onNewStudyGroup(studyGroup)
                }
                .setNegativeButton(R.string.button_cancel) { _, _ ->
                    dismiss()
                }
                .create()
    }

    private fun initViews(layout: View) {
        textinputlayoutCourseView = layout.findViewById(R.id.textinputlayout_course)
        facultyView = layout.findViewById(R.id.actv_dialog_faculty)
        courseView = layout.findViewById(R.id.actv_dialog_course)
        groupView = layout.findViewById(R.id.actv_dialog_group)

        with(groupView) {
            progressBar = layout.findViewById(R.id.progressbar_dialog_group)
            setEnabledFocusable(false)
            setOnItemClickListener { _, _, _, id -> presenter.setGroupValue(id.toInt()) }

            val groupTextInputLayout: TextInputLayout = layout.findViewById(R.id.textinputlayout_group)
            addTextChangedListener(object : SimpleTextWatcher {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    groupTextInputLayout.error = EMPTY_STRING
                    var isEnabled = presenter.isValidGroup(s.toString())
                    if (presenter.isGroupStored(s.toString())) {
                        groupTextInputLayout.error = resources.getString(R.string.group_already_added)
                        isEnabled = false
                    }
                    (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
                }
            })

            setOnClickListener {
                if (presenter.isCourseSet()) {
                    it as LoadingAutoCompleteTextView
                    it.showDropDown()
                }
            }
        }

        with(courseView) {
            setEnabledFocusable(false)
            keyListener = null
            setOnClickListener { courseView.showDropDown() }
            setOnItemClickListener { _, _, position, _ ->
                groupView.progressBarVisibility = View.VISIBLE
                groupView.text.clear()
                groupView.setEnabledFocusable(false)
                presenter.setCourseKeyFromPosition(position)
                presenter.setGroupsNull()
                presenter.getScheduleGroups(showGroupsForAllCourses = false)
            }
        }

        with(facultyView) {
            progressBar = layout.findViewById(R.id.progressbar_dialog_faculty)
            setEnabledFocusable(false)
            keyListener = null
            setOnClickListener { facultyView.showDropDown() }
            setOnItemClickListener { _, _, position, _ ->
                textinputlayoutCourseView.visibility = View.GONE
                groupView.progressBarVisibility = View.VISIBLE
                groupView.text.clear()
                groupView.setEnabledFocusable(false)
                courseView.text.clear()
                presenter.setFacultyKeyFromPosition(position)
                presenter.setCoursesNull()
                presenter.setGroupsNull()
                presenter.getScheduleGroups()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        when {
            presenter.isFacultiesInitialized() -> {
                presenter.populateFacultiesAdapter()
                if (presenter.isCoursesInitialized()) {
                    presenter.populateCoursesAdapter()
                }
                if ((presenter.isFacultySet() && textinputlayoutCourseView.visibility != View.VISIBLE) ||
                        (presenter.isFacultySet() && presenter.isCourseSet() && !presenter.isGroupsInitialized())) {
                    groupView.progressBarVisibility = View.VISIBLE
                }
                if (presenter.isGroupsInitialized()) {
                    presenter.populateGroupsAdapter()
                }
            }
            else -> {
                facultyView.progressBarVisibility = View.VISIBLE
                presenter.getFacultyScheduleFilter()
            }
        }
    }

    override fun showError(t: Throwable) {
        dismiss()
        listener.onError(t)
    }

    override fun populateFacultiesAdapter(data: ScheduleFilter) {
        val adapter = initAdapter(data)
        adapter.isFilteringEnabled = false
        with(facultyView) {
            progressBarVisibility = View.GONE
            setEnabledFocusable(true)
            setAdapter(adapter)
            requestFocus()
        }
    }

    override fun populateCoursesAdapter(data: ScheduleFilter) {
        val adapter = initAdapter(data)
        adapter.isFilteringEnabled = false
        textinputlayoutCourseView.visibility = View.VISIBLE
        groupView.progressBarVisibility = View.GONE
        groupView.text.clear()
        with(courseView) {
            progressBarVisibility = View.GONE
            setEnabledFocusable(true)
            setAdapter(adapter)
            requestFocus()
        }
    }

    override fun populateGroupsAdapter(data: ScheduleFilter) {
        val adapter = initAdapter(data)
        val isStartsWithFilterActive = !presenter.isCourseSet()
        adapter.isStartsWithFilterActive = isStartsWithFilterActive
        adapter.isIgnoreCaseFilterActive = presenter.isCourseSet()
        with(groupView) {
            inputType = if (presenter.isCourseSet()) {
                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            } else {
                InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_VARIATION_NORMAL or
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }
            setTypeface(null, Typeface.NORMAL)
            progressBarVisibility = View.GONE
            setEnabledFocusable(true)
            setAdapter(adapter)
            requestFocus()
        }
    }

    private fun initAdapter(data: ScheduleFilter): ScheduleFilterAdapter {
        return ScheduleFilterAdapter(
                requireActivity(),
                android.R.layout.simple_spinner_dropdown_item,
                data
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
