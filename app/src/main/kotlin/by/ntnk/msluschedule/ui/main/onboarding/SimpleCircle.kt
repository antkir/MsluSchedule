package by.ntnk.msluschedule.ui.main.onboarding

import android.animation.TimeInterpolator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.animation.DecelerateInterpolator
import com.takusemba.spotlight.shape.Shape
import java.util.concurrent.TimeUnit

open class SimpleCircle(
    private val radius: Float,
    override val duration: Long = DEFAULT_DURATION,
    override val interpolator: TimeInterpolator = DEFAULT_INTERPOLATOR
) : Shape {

    override fun draw(canvas: Canvas, point: PointF, value: Float, paint: Paint) {
        canvas.drawCircle(point.x, point.y, value * radius, paint)
    }

    companion object {
        val DEFAULT_DURATION = TimeUnit.MILLISECONDS.toMillis(500)
        val DEFAULT_INTERPOLATOR = DecelerateInterpolator(2f)
    }
}