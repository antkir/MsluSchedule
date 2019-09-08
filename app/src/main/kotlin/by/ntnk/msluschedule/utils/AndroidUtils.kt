package by.ntnk.msluschedule.utils

import android.animation.Animator
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import by.ntnk.msluschedule.R
import com.google.android.material.snackbar.Snackbar
import io.reactivex.subjects.PublishSubject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

val onThemeChanged = PublishSubject.create<Boolean>()

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
        MONDAY -> context.resources.getString(R.string.monday)
        TUESDAY -> context.resources.getString(R.string.tuesday)
        WEDNESDAY -> context.resources.getString(R.string.wednesday)
        THURSDAY -> context.resources.getString(R.string.thursday)
        FRIDAY -> context.resources.getString(R.string.friday)
        SATURDAY -> context.resources.getString(R.string.saturday)
        SUNDAY -> context.resources.getString(R.string.sunday)
        else -> EMPTY_STRING
    }
}

fun Context.dipToPixels(dipValue: Float) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, resources.displayMetrics).toInt()

fun IntRange.random(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        ThreadLocalRandom.current().nextInt((endInclusive + 1) - start) + start
    } else {
        Random().nextInt((endInclusive + 1) - start) + start
    }
}

interface SimpleAnimatorListener : Animator.AnimatorListener {
    override fun onAnimationRepeat(animation: Animator?) = Unit
    override fun onAnimationEnd(animation: Animator?) = Unit
    override fun onAnimationCancel(animation: Animator?) = Unit
    override fun onAnimationStart(animation: Animator?) = Unit
}

interface SimpleTextWatcher : TextWatcher {
    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
    override fun afterTextChanged(s: Editable) = Unit
}

interface SimpleDrawerListener : DrawerLayout.DrawerListener {
    override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit
    override fun onDrawerOpened(drawerView: View) = Unit
    override fun onDrawerClosed(drawerView: View) = Unit
    override fun onDrawerStateChanged(newState: Int) = Unit
}

interface BaseRVItemView {
    val viewType: Int
    fun bindViewHolder(viewHolder: RecyclerView.ViewHolder)
}
