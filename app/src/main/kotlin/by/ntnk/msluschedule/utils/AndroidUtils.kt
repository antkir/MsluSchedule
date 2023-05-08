package by.ntnk.msluschedule.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.TypedValue
import android.view.View
import androidx.collection.SparseArrayCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import by.ntnk.msluschedule.R
import com.google.android.material.snackbar.Snackbar
import io.reactivex.subjects.PublishSubject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Objects
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

val onThemeChanged: PublishSubject<Boolean> = PublishSubject.create()

fun Context.dipToPixels(dipValue: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, resources.displayMetrics).toInt()

fun IntRange.random(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ThreadLocalRandom.current().nextInt((endInclusive + 1) - start) + start
    } else {
        Random().nextInt((endInclusive + 1) - start) + start
    }
}

/**
 * Compares the contents of this [SparseArrayCompat] to the specified [SparseArrayCompat].
 *
 * For backwards compatibility reasons, [Object.equals] cannot be implemented,
 * so this serves as a manually invoked alternative.
 */
fun SparseArrayCompat<*>.contentEquals(other: SparseArrayCompat<*>?): Boolean {
    if (other == null) return false

    if (size() != other.size()) return false
    for (index in 0 until size()) {
        val key = keyAt(index)
        if (valueAt(index) != other[key]) {
            return false
        }
    }

    return true
}

/**
 * Returns a hash code value for the contents of this [SparseArrayCompat], combining the
 * [Objects.hashCode] result of all its keys and values.
 *
 * For backwards compatibility, [Object.hashCode] cannot be implemented, so this serves
 * as a manually invoked alternative.
 */
fun SparseArrayCompat<*>.contentHashCode(): Int {
    var hash = 0
    for (index in 0 until size()) {
        val key = keyAt(index)
        val value = valueAt(index)
        hash = 31 * hash + Objects.hashCode(key)
        hash = 31 * hash + Objects.hashCode(value)
    }
    return hash
}

interface SimpleDrawerListener : DrawerLayout.DrawerListener {
    override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit
    override fun onDrawerOpened(drawerView: View) = Unit
    override fun onDrawerClosed(drawerView: View) = Unit
    override fun onDrawerStateChanged(newState: Int) = Unit
}

object AndroidUtils {
    fun isNetworkAccessible(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val activeNetwork = connectivityManager?.activeNetwork
            val networkCapabilities = connectivityManager?.getNetworkCapabilities(activeNetwork) ?: return false
            return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            return connectivityManager?.activeNetworkInfo != null &&
                connectivityManager.activeNetworkInfo?.isConnected == true
        }
    }

    fun showSnackbarNetworkInaccessible(view: View) {
        val snackbar = Snackbar.make(view, R.string.snackbar_internet_unavailable, Snackbar.LENGTH_LONG)
        snackbar.anchorView = view
        ViewCompat.setOnApplyWindowInsetsListener(snackbar.view) { _, insets -> insets }
        snackbar.show()
    }

    fun getErrorMessageResId(t: Throwable): Int {
        return when (t) {
            is ConnectException -> R.string.error_website_unavailable
            is HttpStatusException -> R.string.error_website_unavailable
            is SocketTimeoutException -> R.string.error_website_unavailable
            is UnknownHostException -> R.string.error_website_unavailable
            else -> R.string.error_general
        }
    }

    fun getWeekdayFromTag(weekdayTag: String, context: Context): String {
        return when (weekdayTag) {
            Days.MONDAY -> context.resources.getString(R.string.monday)
            Days.TUESDAY -> context.resources.getString(R.string.tuesday)
            Days.WEDNESDAY -> context.resources.getString(R.string.wednesday)
            Days.THURSDAY -> context.resources.getString(R.string.thursday)
            Days.FRIDAY -> context.resources.getString(R.string.friday)
            Days.SATURDAY -> context.resources.getString(R.string.saturday)
            Days.SUNDAY -> context.resources.getString(R.string.sunday)
            else -> EMPTY_STRING
        }
    }
}
