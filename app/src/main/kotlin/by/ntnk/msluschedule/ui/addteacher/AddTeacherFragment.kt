package by.ntnk.msluschedule.ui.addteacher

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.Teacher
import by.ntnk.msluschedule.mvp.views.MvpDialogFragment
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.ui.adapters.ScheduleFilterAdapter
import by.ntnk.msluschedule.ui.customviews.LoadingAutoCompleteTextView
import by.ntnk.msluschedule.utils.uiScheduler
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddTeacherFragment : MvpDialogFragment<AddTeacherPresenter, AddTeacherView>(), AddTeacherView {
    private lateinit var listener: OnPositiveButtonListener
    private lateinit var teacherView: LoadingAutoCompleteTextView

    override val view: AddTeacherView
        get() = this

    @Inject
    lateinit var injectedPresenter: Lazy<AddTeacherPresenter>

    override fun onCreatePresenter(): AddTeacherPresenter = injectedPresenter.get()

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
        try {
            listener = context as OnPositiveButtonListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() +
                    " must implement OnPositiveButtonListener")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = View.inflate(activity, R.layout.add_teacher_view, null)
        initViews(layout)
        getData(savedInstanceState)
        val dialog = initMaterialDialog(layout)
        dialog.setOnShowListener {
            (it as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    presenter.isValidTeacher(teacherView.text.toString())
        }
        return dialog
    }

    private fun initViews(layout: View) {
        teacherView = layout.findViewById(R.id.actv_dialog_teacher)
        teacherView.progressBar = layout.findViewById(R.id.progressbar_dialog_teacher)
        teacherView.setEnabledFocusable(false)
        teacherView.setOnItemClickListener { _, _, _, id -> presenter.setTeacherValue(id.toInt()) }
        teacherView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val isEnabled = presenter.isValidTeacher(s.toString())
                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun getData(savedInstanceState: Bundle?) {
        when {
            savedInstanceState == null -> {
                teacherView.progressBarVisibility = View.VISIBLE
                presenter.getTeachersScheduleFilter(uiScheduler)
            }
            presenter.isTeachersNotEmpty -> presenter.populateTeachersAdapter()
            else -> dismiss()
        }
    }

    private fun initMaterialDialog(layout: View): Dialog {
        return AlertDialog.Builder(activity!!, R.style.MsluTheme_Dialog_Alert)
                .setTitle((R.string.add_teacher_title))
                .setView(layout)
                .setPositiveButton(R.string.button_add) { _, _ ->
                    listener.onPositiveButtonTeacher(presenter.getTeacher())
                }
                .setNegativeButton(R.string.button_cancel) { _, _ ->
                    dismiss()
                }
                .create()
    }

    override fun populateTeachersView(data: ScheduleFilter) {
        teacherView.progressBarVisibility = View.GONE
        teacherView.setEnabledFocusable(true)
        val adapter = initAdapter(data)
        adapter.isIgnoreCaseFilter = true
        teacherView.setAdapter(adapter)
        teacherView.requestFocus()
    }

    private fun initAdapter(response: ScheduleFilter): ScheduleFilterAdapter {
        return ScheduleFilterAdapter(
                activity!!,
                android.R.layout.simple_spinner_dropdown_item,
                response
        )
    }

    override fun showError(t: Throwable) {
        TODO("not implemented")
    }

    override fun dismiss() {
        presenter.clearDisposables()
        super.dismiss()
    }

    interface OnPositiveButtonListener {
        fun onPositiveButtonTeacher(teacher: Teacher)
    }
}
