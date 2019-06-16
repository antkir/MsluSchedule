package by.ntnk.msluschedule.mvp

import android.util.SparseArray
import by.ntnk.msluschedule.di.PerApp
import javax.inject.Inject

@PerApp
class PresenterManager @Inject constructor() {
    private val presenters = SparseArray<Presenter<*>>()

    internal fun getPresenter(id: Int): Presenter<*>? {
        return presenters[id]
    }

    internal fun addPresenter(id: Int, presenter: Presenter<*>) {
        presenters.put(id, presenter)
    }

    internal fun removePresenter(id: Int) {
        presenters.remove(id)
    }
}
