package by.ntnk.msluschedule.mvp

class PresenterManager {
    private val presenters = HashMap<Int, Presenter<*>>()
    private var presenterId: Int = 0

    internal fun getPresenter(id: Int): Presenter<*>? {
        return presenters[id]
    }

    internal fun addPresenter(presenter: Presenter<*>): Int {
        presenters[++presenterId] = presenter
        return presenterId
    }

    internal fun removePresenter(id: Int) {
        presenters.remove(id)
    }
}
