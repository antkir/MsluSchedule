package by.ntnk.msluschedule.ui.addteacher

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.mvp.views.MvpDialogFragment
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddTeacherFragment : MvpDialogFragment<AddTeacherPresenter, AddTeacherView>(), AddTeacherView {
    @Inject
    lateinit var injectedPresenter: AddTeacherPresenter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = View.inflate(activity, R.layout.add_teacher_view, null)
        return initMaterialDialog(layout)
    }

    private fun initMaterialDialog(layout: View): Dialog {
        return AlertDialog.Builder(activity!!)
                .setTitle((R.string.add_teacher_title))
                .setView(layout)
                .setPositiveButton(R.string.button_add) { _, _ ->
                    dismiss()
                }
                .setNegativeButton(R.string.button_cancel) { _, _ ->
                    dismiss()
                }
                .create()
    }

    override fun onCreatePresenter(): AddTeacherPresenter {
        return injectedPresenter
    }

    override val view: AddTeacherView
        get() = this

    override fun populateTeachersView(data: HashMap<Int, String>) {
        TODO("not implemented")
    }

    override fun showError(t: Throwable) {
        TODO("not implemented")
    }
}
