package by.ntnk.msluschedule.mvp.views

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.mvp.PresenterManager
import by.ntnk.msluschedule.mvp.View
import by.ntnk.msluschedule.utils.PRESENTER_ID_KEY
import javax.inject.Inject

/**
 * Generic activity, extended to work with MVP pattern.
 *
 * @param P the type of a presenter for this activity.
 * @param V the type of a view, representing this activity.
 */
abstract class MvpActivity<out P : Presenter<V>, V : View> : AppCompatActivity() {
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

    /** Called if the presenter for this activity hasn't been created yet. */
    protected abstract fun onCreatePresenter(): P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenterId = savedInstanceState?.getSerializable(PRESENTER_ID_KEY) as Int?
        if (retainPresenter) {
            if (isInvalidPresenter()) presenterId = null
            if (presenterId == null) {
                presenterId = presenterManager.addPresenter(hashCode(), presenter)
            }
        }
        presenter.bindView(view)
    }

    @Suppress("UNCHECKED_CAST")
    private fun isInvalidPresenter(): Boolean {
        if (presenterId != null) {
            if (presenterManager.getPresenter(presenterId!!) == null) {
                return true
            }
        }
        return false
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
        if (retainPresenter && presenterId != null && !isChangingConfigurations) {
            presenterManager.removePresenter(presenterId!!)
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(PRESENTER_ID_KEY, presenterId)
        super.onSaveInstanceState(outState)
    }
}
