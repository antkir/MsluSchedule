package by.ntnk.msluschedule

import timber.log.Timber

class AppTimberTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) = Unit
}
