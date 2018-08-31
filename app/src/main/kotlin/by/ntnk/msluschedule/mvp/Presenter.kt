package by.ntnk.msluschedule.mvp

open class Presenter<V : View> {
    protected var view: V? = null

    internal fun bindView(view: V) {
        this.view = view
    }

    internal fun unbindView() {
        this.view = null
    }
}
