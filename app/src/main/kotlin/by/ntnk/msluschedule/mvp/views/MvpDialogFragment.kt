package by.ntnk.msluschedule.mvp.views

import android.os.Bundle
import android.support.v4.app.DialogFragment
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.mvp.PresenterManager
import by.ntnk.msluschedule.mvp.View
import by.ntnk.msluschedule.utils.PRESENTER_ID_KEY
import javax.inject.Inject

abstract class MvpDialogFragment<out P : Presenter<V>, V : View> : DialogFragment() {
    private var presenterId: Int? = null
    private lateinit var presenterManager: PresenterManager

    @Inject
    fun setPresenter(presenterManager: PresenterManager) {
        this.presenterManager = presenterManager
    }

    @Suppress("UNCHECKED_CAST")
    protected val presenter: P by lazy {
        if (presenterId != null) {
            presenterManager.getPresenter(presenterId!!) as P? ?: onCreatePresenter()
        } else {
            onCreatePresenter()
        }
    }

    protected abstract val view: V

    protected abstract fun onCreatePresenter(): P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenterId = restoreId(savedInstanceState)
        attachViewToPresenter()
    }

    private fun restoreId(savedInstanceState: Bundle?): Int? {
        return savedInstanceState?.getSerializable(PRESENTER_ID_KEY) as Int?
    }

    private fun attachViewToPresenter() {
        if (presenterId == null) {
            presenterId = presenterManager.addPresenter(presenter)
        }
        presenter.bindView(view)
    }

    override fun onDestroy() {
        super.onDestroy()
        detachViewFromPresenter()
        if (presenterId != null && !activity!!.isChangingConfigurations) {
            presenterManager.removePresenter(presenterId!!)
        }
    }

    private fun detachViewFromPresenter() {
        presenter.unbindView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(PRESENTER_ID_KEY, presenterId)
        super.onSaveInstanceState(outState)
    }
}
