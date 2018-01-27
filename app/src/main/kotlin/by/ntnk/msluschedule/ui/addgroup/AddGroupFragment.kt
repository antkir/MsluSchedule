package by.ntnk.msluschedule.ui.addgroup

import android.content.Context
import by.ntnk.msluschedule.mvp.views.MvpDialogFragment
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

class AddGroupFragment : MvpDialogFragment<AddGroupPresenter, AddGroupView>(), AddGroupView {
    @Inject
    lateinit var injectedPresenter: AddGroupPresenter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreatePresenter(): AddGroupPresenter {
        return injectedPresenter
    }

    override val view: AddGroupView
        get() = this

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
