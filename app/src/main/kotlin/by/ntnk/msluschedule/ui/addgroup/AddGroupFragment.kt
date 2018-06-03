package by.ntnk.msluschedule.ui.addgroup

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.data.StudyGroup
import by.ntnk.msluschedule.mvp.views.MvpDialogFragment
import by.ntnk.msluschedule.network.data.ScheduleFilter
import by.ntnk.msluschedule.ui.adapters.ScheduleFilterAdapter
import by.ntnk.msluschedule.ui.customviews.LoadingAutoCompleteTextView
import dagger.Lazy
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddGroupFragment : MvpDialogFragment<AddGroupPresenter, AddGroupView>(), AddGroupView {
    private lateinit var listener: OnPositiveButtonListener
    private lateinit var facultyView: LoadingAutoCompleteTextView
    private lateinit var groupView: LoadingAutoCompleteTextView

    override val view: AddGroupView
        get() = this

    @Inject
    lateinit var injectedPresenter: Lazy<AddGroupPresenter>

    override fun onCreatePresenter(): AddGroupPresenter = injectedPresenter.get()

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
        val layout = View.inflate(activity, R.layout.add_group_view, null)
        initViews(layout)
        getData(savedInstanceState)
        val dialog = initMaterialDialog(layout)
        dialog.window.attributes.windowAnimations = R.style.DialogAnimation
        dialog.setOnShowListener {
            (it as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    presenter.isValidGroup(groupView.text.toString())
        }
        return dialog
    }

    private fun getData(savedInstanceState: Bundle?) {
        when {
            savedInstanceState == null -> {
                facultyView.progressBarVisibility = View.VISIBLE
                presenter.getFacultyScheduleFilter()
            }
            presenter.isFacultiesNotEmpty -> {
                presenter.populateFacultiesAdapter()
                if (presenter.isFacultySelected) groupView.progressBarVisibility = View.VISIBLE
                if (presenter.isGroupsNotEmpty) presenter.populateGroupsAdapter()
            }
            else -> dismiss()
        }
    }

    private fun initMaterialDialog(layout: View): Dialog {
        return AlertDialog.Builder(activity!!, R.style.MsluTheme_Dialog_Alert)
                .setTitle((R.string.add_group_title))
                .setView(layout)
                .setPositiveButton(R.string.button_add) { _, _ ->
                    listener.onPositiveButtonGroup(presenter.getStudyGroup())
                }
                .setNegativeButton(R.string.button_cancel) { _, _ ->
                    dismiss()
                }
                .create()
    }

    private fun initViews(layout: View) {
        facultyView = layout.findViewById(R.id.actv_dialog_faculty)
        facultyView.progressBar = layout.findViewById(R.id.progressbar_dialog_faculty)
        groupView = layout.findViewById(R.id.actv_dialog_group)
        groupView.progressBar = layout.findViewById(R.id.progressbar_dialog_group)

        facultyView.setEnabledFocusable(false)
        groupView.setEnabledFocusable(false)

        facultyView.keyListener = null
        facultyView.setOnClickListener { _ -> facultyView.showDropDown() }
        facultyView.setOnItemClickListener { _, _, position, _ ->
            groupView.progressBarVisibility = View.VISIBLE
            presenter.setFacultyValueFromPosition(position)
            presenter.getScheduleGroups()
            groupView.text.clear()
        }

        groupView.setOnItemClickListener { _, _, _, id -> presenter.setGroupValue(id.toInt()) }
        groupView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val isEnabled = presenter.isValidGroup(s.toString())
                (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = isEnabled
            }

            override fun afterTextChanged(s: Editable) {
            }
        })
    }

    override fun showError(t: Throwable) {
        TODO("not implemented")
    }

    override fun populateFacultiesAdapter(data: ScheduleFilter) {
        facultyView.progressBarVisibility = View.GONE
        facultyView.setEnabledFocusable(true)
        val adapter = initAdapter(data)
        adapter.isFilteringEnabled = false
        facultyView.setAdapter(adapter)
        facultyView.requestFocus()
    }

    override fun populateGroupsAdapter(data: ScheduleFilter) {
        groupView.progressBarVisibility = View.GONE
        groupView.setEnabledFocusable(true)
        val adapter = initAdapter(data)
        adapter.isStartsWithFilter = true
        groupView.setAdapter(adapter)
        groupView.requestFocus()
    }

    private fun initAdapter(response: ScheduleFilter): ScheduleFilterAdapter {
        return ScheduleFilterAdapter(
                activity!!,
                android.R.layout.simple_spinner_dropdown_item,
                response
        )
    }

    override fun dismiss() {
        presenter.clearDisposables()
        super.dismiss()
    }

    interface OnPositiveButtonListener {
        fun onPositiveButtonGroup(studyGroup: StudyGroup)
    }
}
