package by.ntnk.msluschedule.mvp

import androidx.collection.SparseArrayCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresenterManager @Inject constructor() {
    private val presenters = SparseArrayCompat<Presenter<*>>()

    internal fun getPresenter(id: Int): Presenter<*>? {
        return presenters[id]
    }

    internal fun addPresenter(id: Int, presenter: Presenter<*>): Int {
        presenters.put(id, presenter)
        return id
    }

    internal fun removePresenter(id: Int) {
        presenters.remove(id)
    }
}
