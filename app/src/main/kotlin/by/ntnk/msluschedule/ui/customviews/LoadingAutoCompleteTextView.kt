package by.ntnk.msluschedule.ui.customviews

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class LoadingAutoCompleteTextView : MaterialAutoCompleteTextView {
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
}
