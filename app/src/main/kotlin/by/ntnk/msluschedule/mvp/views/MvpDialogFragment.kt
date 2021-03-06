package by.ntnk.msluschedule.mvp.views

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import by.ntnk.msluschedule.mvp.Presenter
import by.ntnk.msluschedule.mvp.PresenterManager
import by.ntnk.msluschedule.mvp.View
import by.ntnk.msluschedule.utils.PRESENTER_ID_KEY
import by.ntnk.msluschedule.utils.random
import javax.inject.Inject
import kotlin.math.absoluteValue

/**
 * Generic dialog fragment, extended to work with MVP pattern.
 *
 * @param P the type of a [Presenter] for this dialog fragment.
 * @param V the type of a [View], representing this dialog fragment.
 */
abstract class MvpDialogFragment<out P : Presenter<V>, V : View> : DialogFragment() {
    private var presenterId: Int? = null
    private lateinit var presenterManager: PresenterManager

    @Inject
    fun setPresenterManager(presenterManager: PresenterManager) {
        this.presenterManager = presenterManager
    }

    /** Gets the presenter from [PresenterManager] or creates a new one, if it wasn't found there. */
    protected val presenter: P
        get() =
            if (presenterId != null) {
                @Suppress("UNCHECKED_CAST")
                // Have a fail-safe path here, just in case.
                presenterManager.getPresenter(presenterId!!) as? P ?: onCreatePresenter().also { presenterId = null }
            } else {
                onCreatePresenter()
            }

    /** Represents this [DialogFragment]. */
    protected abstract val view: V

    /** Called if the presenter for this dialog fragment hasn't been created yet. */
    protected abstract fun onCreatePresenter(): P

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenterId = savedInstanceState?.getSerializable(PRESENTER_ID_KEY) as Int?
        validatePresenter()
        if (presenterId == null) {
            val hashCode = toString().hashCode().absoluteValue
            val id = (0 until Int.MAX_VALUE - hashCode).random() + hashCode
            presenterId = presenterManager.addPresenter(id, presenter)
        }
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
        if (presenterId != null && !requireActivity().isChangingConfigurations) {
            presenterManager.removePresenter(presenterId!!)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(PRESENTER_ID_KEY, presenterId)
    }
}
