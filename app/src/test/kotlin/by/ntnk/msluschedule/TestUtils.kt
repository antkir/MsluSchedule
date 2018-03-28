package by.ntnk.msluschedule

import org.mockito.Mockito
import timber.log.Timber

class TestTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        println(message)
    }
}

@Suppress("unchecked_cast")
fun <T> any(): T {
    Mockito.any<T>()
    return null as T
}
