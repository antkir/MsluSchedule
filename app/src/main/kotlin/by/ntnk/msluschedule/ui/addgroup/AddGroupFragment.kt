package by.ntnk.msluschedule.ui.addgroup

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.mvp.views.MvpDialogFragment
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddGroupFragment : MvpDialogFragment<AddGroupPresenter, AddGroupView>(), AddGroupView {
    override val view: AddGroupView
        get() = this

    @Inject
    lateinit var injectedPresenter: AddGroupPresenter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layout = View.inflate(activity, R.layout.add_group_view, null)
        return initMaterialDialog(layout)
    }

    private fun initMaterialDialog(layout: View): Dialog {
        return AlertDialog.Builder(activity!!)
                .setTitle((R.string.add_group_title))
                .setView(layout)
                .setPositiveButton(R.string.button_add) { _, _ ->
                    dismiss()
                }
                .setNegativeButton(R.string.button_cancel) { _, _ ->
                    dismiss()
                }
                .create()
    }

    override fun onCreatePresenter(): AddGroupPresenter {
        return injectedPresenter
    }

    override fun showError(t: Throwable) {
        TODO("not implemented")
    }

    override fun populateFacultiesView(data: HashMap<Int, String>) {
        TODO("not implemented")
    }

    override fun populateGroupsView(data: HashMap<Int, String>) {
        TODO("not implemented")
    }
}
