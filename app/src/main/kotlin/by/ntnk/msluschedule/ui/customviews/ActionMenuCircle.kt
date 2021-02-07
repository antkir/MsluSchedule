package by.ntnk.msluschedule.ui.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import by.ntnk.msluschedule.R
import by.ntnk.msluschedule.utils.dipToPixels
import com.google.android.material.appbar.MaterialToolbar
import com.takusemba.spotlight.shape.Circle
import kotlin.math.roundToInt

class ActionMenuCircle(radius: Float, context: Context, private val toolbar: MaterialToolbar) : Circle(radius) {
    private val moreIcon = ContextCompat.getDrawable(context, R.drawable.ic_more) as Drawable
    private val actionMenuEndMargin = context.dipToPixels(10f)

    override fun draw(canvas: Canvas, point: PointF, value: Float, paint: Paint) {
        super.draw(canvas, point, value, paint)
        val toolbarLocation = IntArray(2)
        toolbar.getLocationOnScreen(toolbarLocation)
        val topMargin = (toolbar.height - moreIcon.intrinsicHeight) / 2
        val deleteIconTop = toolbarLocation[1] + topMargin
        val deleteIconEnd = toolbar.width - actionMenuEndMargin
        val deleteIconStart = deleteIconEnd - moreIcon.intrinsicWidth
        val deleteIconBottom = deleteIconTop + moreIcon.intrinsicHeight
        moreIcon.setBounds(deleteIconStart, deleteIconTop, deleteIconEnd, deleteIconBottom)
        moreIcon.alpha = if (value > 0.5f) (value * 255).roundToInt() else 0
        moreIcon.draw(canvas)
    }
}
