package by.ntnk.msluschedule

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class AppTimberTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority >= Log.WARN) {
            FirebaseCrashlytics.getInstance().log(message)
            if (t != null) {
                FirebaseCrashlytics.getInstance().recordException(t)
            }
        }
    }
}
