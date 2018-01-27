package by.ntnk.msluschedule.ui.addteacher

import android.content.Context
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
