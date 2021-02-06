package by.ntnk.msluschedule.ui.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import com.google.android.material.progressindicator.CircularProgressIndicator

class LoadingAutoCompleteTextView : AppCompatAutoCompleteTextView {
    var progressBar: CircularProgressIndicator? = null
    var progressBarVisibility: Int
        get() = progressBar?.visibility!!
        set(value) {
            progressBar?.visibility = value
        }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setEnabledFocusable(focusable: Boolean) {
        isEnabled = focusable
        isFocusable = focusable
        isFocusableInTouchMode = focusable
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && isPopupShowing) {
            val inputManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (inputManager.hideSoftInputFromWindow(findFocus().windowToken, InputMethodManager.HIDE_NOT_ALWAYS)) {
                return true
            }
        }
        return super.onKeyPreIme(keyCode, event)
    }
}
