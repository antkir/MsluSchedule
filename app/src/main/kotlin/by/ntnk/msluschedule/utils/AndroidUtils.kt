package by.ntnk.msluschedule.utils

import android.animation.Animator
import android.content.Context
import android.net.ConnectivityManager
import android.support.design.widget.Snackbar
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import by.ntnk.msluschedule.R

fun isNetworkAccessible(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    return connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo.isConnected
}

fun showSnackbarNetworkInaccessible(view: View) =
        Snackbar.make(view, R.string.snackbar_internet_unavailable, Snackbar.LENGTH_LONG).show()

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
