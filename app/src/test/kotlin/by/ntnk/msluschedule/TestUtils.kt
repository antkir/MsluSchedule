package by.ntnk.msluschedule

import org.mockito.Mockito
import timber.log.Timber

class TestTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        println(message)
    }
}

inline fun <reified T> any(): T {
    return Mockito.any(T::class.java)
}
