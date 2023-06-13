package by.ntnk.msluschedule.ui.main.onboarding

import android.animation.TimeInterpolator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.utils.dipToPixels
import com.google.android.material.appbar.MaterialToolbar
import kotlin.math.roundToInt

class ActionMenuCircle(
    context: Context,
    private val toolbar: MaterialToolbar,
    radius: Float,
    override val duration: Long = DEFAULT_DURATION,
    override val interpolator: TimeInterpolator = DEFAULT_INTERPOLATOR
) : SimpleCircle(radius, duration, interpolator) {

    private val moreIcon = ContextCompat.getDrawable(context, R.drawable.ic_more) as Drawable
    private val actionMenuXMargin = context.dipToPixels(10f)

    override fun draw(canvas: Canvas, point: PointF, value: Float, paint: Paint) {
        super.draw(canvas, point, value, paint)

        val toolbarLocation = IntArray(2)
        toolbar.getLocationOnScreen(toolbarLocation)
        val topMargin = (toolbar.height - moreIcon.intrinsicHeight) / 2
        val iconTop = toolbarLocation[1] + topMargin
        val iconEnd = toolbar.width - actionMenuXMargin
        val iconStart = iconEnd - moreIcon.intrinsicWidth
        val iconBottom = iconTop + moreIcon.intrinsicHeight
        moreIcon.setBounds(iconStart, iconTop, iconEnd, iconBottom)
        moreIcon.alpha = if (value > 0.5f) (value * 255).roundToInt() else 0
        moreIcon.draw(canvas)
    }
}
