package by.ntnk.msluschedule.mvp

import java.util.concurrent.atomic.AtomicInteger

class PresenterManager {
    private val presenters = HashMap<Int, Presenter<*>>()
    private val atomicId = AtomicInteger()

    internal fun getPresenter(id: Int): Presenter<*>? {
        return presenters[id]
    }

    internal fun addPresenter(presenter: Presenter<*>): Int {
        val id = atomicId.incrementAndGet()
        presenters[id] = presenter
        return id
    }

    internal fun removePresenter(id: Int) {
        presenters.remove(id)
    }
}
