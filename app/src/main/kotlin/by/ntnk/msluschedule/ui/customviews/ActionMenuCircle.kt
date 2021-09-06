package by.ntnk.msluschedule.ui.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.utils.dipToPixels
import com.google.android.material.appbar.MaterialToolbar
import com.takusemba.spotlight.shape.Circle
import java.util.Locale
import kotlin.math.roundToInt

class ActionMenuCircle(radius: Float, context: Context, private val toolbar: MaterialToolbar) : Circle(radius) {
    private val moreIcon = ContextCompat.getDrawable(context, R.drawable.ic_more) as Drawable
    private val actionMenuXMargin = context.dipToPixels(10f)

    override fun draw(canvas: Canvas, point: PointF, value: Float, paint: Paint) {
        super.draw(canvas, point, value, paint)
        val layoutDirection = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
        val isRTL = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL
        val toolbarLocation = IntArray(2)
        toolbar.getLocationOnScreen(toolbarLocation)
        val topMargin = (toolbar.height - moreIcon.intrinsicHeight) / 2
        val iconTop = toolbarLocation[1] + topMargin
        val iconEnd = if (isRTL) actionMenuXMargin + moreIcon.intrinsicWidth else toolbar.width - actionMenuXMargin
        val iconStart = iconEnd - moreIcon.intrinsicWidth
        val iconBottom = iconTop + moreIcon.intrinsicHeight
        moreIcon.setBounds(iconStart, iconTop, iconEnd, iconBottom)
        moreIcon.alpha = if (value > 0.5f) (value * 255).roundToInt() else 0
        moreIcon.draw(canvas)
    }
}
