package by.ntnk.msluschedule.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.TypedValue
import android.view.View
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import by.ntnk.msluschedule.R
import com.google.android.material.snackbar.Snackbar
import io.reactivex.subjects.PublishSubject
import org.jsoup.HttpStatusException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.ThreadLocalRandom

val onThemeChanged: PublishSubject<Boolean> = PublishSubject.create()

fun Context.dipToPixels(dipValue: Float) =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, resources.displayMetrics).toInt()

fun IntRange.random(): Int {
    return ThreadLocalRandom.current().nextInt((endInclusive + 1) - start) + start
}

fun View.getWidthMinusPadding(): Int {
    return width - paddingLeft - paddingRight
}

interface SimpleDrawerListener : DrawerLayout.DrawerListener {
    override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit
    override fun onDrawerOpened(drawerView: View) = Unit
    override fun onDrawerClosed(drawerView: View) = Unit
    override fun onDrawerStateChanged(newState: Int) = Unit
}

object AndroidUtils {

    const val SNACKBAR_LENGTH_LONGER = 5000

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

    fun getErrorMessageResId(throwable: Throwable): Int {
        return when (throwable) {
            is ConnectException -> R.string.error_website_unavailable
            is HttpStatusException -> R.string.error_website_unavailable
            is SocketTimeoutException -> R.string.error_website_unavailable
            is UnknownHostException -> R.string.error_website_unavailable
            is NetworkApiVersionException -> R.string.error_network_api_version
            is NoDataOnServerException -> R.string.error_no_data_on_server
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
