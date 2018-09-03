package by.ntnk.msluschedule.utils

import android.animation.Animator
import android.content.Context
import android.net.ConnectivityManager
import android.support.design.widget.Snackbar
import android.support.v4.widget.DrawerLayout
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import by.ntnk.msluschedule.R
import java.net.ConnectException
import java.net.SocketTimeoutException

fun isNetworkAccessible(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo.isConnected
}

fun showSnackbarNetworkInaccessible(view: View) =
        Snackbar.make(view, R.string.snackbar_internet_unavailable, Snackbar.LENGTH_LONG).show()

fun getErrorMessageResId(t: Throwable): Int {
    return when (t) {
        is ConnectException -> R.string.error_website_unavailable
        is HttpStatusException -> R.string.error_website_unavailable
        is SocketTimeoutException -> R.string.error_website_unavailable
        else -> R.string.error_general
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