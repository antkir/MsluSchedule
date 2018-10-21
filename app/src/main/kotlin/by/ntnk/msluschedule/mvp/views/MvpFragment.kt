package by.ntnk.msluschedule.mvp.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.mvp.PresenterManager
import by.ntnk.msluschedule.mvp.View
import by.ntnk.msluschedule.utils.PRESENTER_ID_KEY
import by.ntnk.msluschedule.utils.random
import javax.inject.Inject

/**
 * Generic fragment, extended to work with MVP pattern.
 *
 * @param P the type of a presenter for this fragment.
 * @param V the type of a view, representing this fragment.
 */
abstract class MvpFragment<out P : Presenter<V>, V : View> : Fragment() {
    private var presenterId: Int? = null
    private lateinit var presenterManager: PresenterManager

    @Inject
    fun setPresenter(presenterManager: PresenterManager) {
        this.presenterManager = presenterManager
    }

    /** Gets the presenter from [PresenterManager] or creates a new one, if it wasn't found there. */
    @Suppress("UNCHECKED_CAST")
    protected val presenter: P
        get() =
            if (presenterId != null) {
                presenterManager.getPresenter(presenterId!!) as P
            } else {
                onCreatePresenter()
            }

    protected abstract val view: V

    private var retainPresenter: Boolean = true

    /** Called if the presenter for this fragment hasn't been created yet. */
    protected abstract fun onCreatePresenter(): P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenterId = savedInstanceState?.getSerializable(PRESENTER_ID_KEY) as Int?
        if (retainPresenter) {
            validatePresenter()
            if (presenterId == null) {
                val id = (0 until Int.MAX_VALUE).random()
                presenterId = presenterManager.addPresenter(id, presenter)
            }
        }
        presenter.bindView(view)
    }

    private fun validatePresenter() {
        if (presenterId != null && presenterManager.getPresenter(presenterId!!) == null) {
            presenterManager.removePresenter(presenterId!!)
            presenterId = null
        }
    }

    override fun onStart() {
        super.onStart()
        presenter.bindView(view)
    }

    override fun onStop() {
        super.onStop()
        presenter.unbindView()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (retainPresenter && presenterId != null && !activity!!.isChangingConfigurations) {
            presenterManager.removePresenter(presenterId!!)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(PRESENTER_ID_KEY, presenterId)
    }
}
