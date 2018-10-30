package by.ntnk.msluschedule

import android.util.Log
import com.crashlytics.android.Crashlytics
import timber.log.Timber

class AppTimberTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < Log.WARN) return
        Crashlytics.log(priority, tag, message)
        if (t != null) {
            Crashlytics.logException(t)
        }
    }
}
