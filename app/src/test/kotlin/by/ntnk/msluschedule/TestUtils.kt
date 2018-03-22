package by.ntnk.msluschedule

import timber.log.Timber

class TestTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        println(message)
    }
}
