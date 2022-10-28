package by.ntnk.msluschedule.ui.addteacher

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Teacher
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

class AddTeacherFragment : MvpDialogFragment<AddTeacherPresenter, AddTeacherView>(), AddTeacherView {

    private lateinit var listener: DialogListener
    private lateinit var teacherView: LoadingAutoCompleteTextView

    override val view: AddTeacherView
        get() = this

    @Inject
    lateinit var injectedPresenter: Lazy<AddTeacherPresenter>

    override fun onCreatePresenter(): AddTeacherPresenter = injectedPresenter.get()

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
        val layout = View.inflate(activity, R.layout.add_teacher_view, null)
        initViews(layout)
        val dialog = initMaterialDialog(layout)
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.setOnShowListener {
            (it as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                presenter.isValidTeacher(teacherView.text.toString())
        }
        return dialog
    }

    private fun initViews(layout: View) {
        teacherView = layout.findViewById(R.id.actv_dialog_teacher)
        teacherView.progressBar = layout.findViewById(R.id.progressbar_dialog_teacher)
        val teacherTextInputLayout: TextInputLayout = layout.findViewById(R.id.textinputlayout_teacher)
        teacherView.setEnabledFocusable(false)
        teacherView.setOnItemClickListener { _, _, _, id -> presenter.setTeacherValue(id.toInt()) }
        teacherView.addTextChangedListener(object : SimpleTextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                teacherTextInputLayout.error = EMPTY_STRING
                var isEnabled = presenter.isValidTeacher(s.toString())
                if (presenter.isTeacherStored(s.toString())) {
                    teacherTextInputLayout.error = resources.getString(R.string.teacher_already_added)
                    isEnabled = false
                }
                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
            }
        })
    }

    private fun initMaterialDialog(layout: View): Dialog {
        return MaterialAlertDialogBuilder(requireActivity(), R.style.MsluTheme_Dialog_Alert)
            .setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.rounded_corners_rect))
            .setTitle((R.string.add_teacher_title))
            .setView(layout)
            .setPositiveButton(R.string.button_add) { _, _ ->
                listener.onNewTeacher(presenter.getTeacher())
            }
            .setNegativeButton(R.string.button_cancel) { _, _ ->
                dismiss()
            }
            .create()
    }

    override fun onStart() {
        super.onStart()
        when {
            presenter.isTeachersNotEmpty() -> presenter.populateTeachersAdapter()
            else -> {
                teacherView.progressBarVisibility = View.VISIBLE
                presenter.getTeachersScheduleFilter()
            }
        }
    }

    override fun populateTeachersView(data: ScheduleFilter) {
        teacherView.progressBarVisibility = View.GONE
        teacherView.setEnabledFocusable(true)
        val adapter = initAdapter(data)
        adapter.isIgnoreCaseFilterActive = true
        teacherView.setAdapter(adapter)
        teacherView.requestFocus()
    }

    private fun initAdapter(data: ScheduleFilter): ScheduleFilterAdapter {
        return ScheduleFilterAdapter(
            requireActivity(),
            android.R.layout.simple_spinner_dropdown_item,
            data
        )
    }

    override fun showError(t: Throwable) {
        dismiss()
        listener.onError(t)
    }

    override fun dismiss() {
        super.dismiss()
        presenter.clearDisposables()
    }

    interface DialogListener {
        fun onNewTeacher(teacher: Teacher)
        fun onError(t: Throwable)
    }
}
