package by.ntnk.msluschedule.ui.main.onboarding

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.PointF
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.window.layout.WindowMetricsCalculator
import by.ntnk.msluschedule.R

@SuppressLint("ViewConstructor")
class OverlayLayout(
    context: Context,
    anchorPoint: PointF,
    shapeHalfSize: Float,
    title: String,
    description: String
) : FrameLayout(context) {

    init {
        val activity = (context as Activity)
        val overlayView = activity.layoutInflater.inflate(R.layout.layout_onboarding, this)

        val titleTextView = overlayView.findViewById<TextView>(R.id.text_title)
        titleTextView.text = title
        val descriptionTextView = overlayView.findViewById<TextView>(R.id.text_description)
        descriptionTextView.text = description

        val overlayPadding = resources.getDimension(R.dimen.overlay_onboarding_padding).toInt()

        val layout = overlayView.findViewById<LinearLayout>(R.id.container)
        layout.setPadding(overlayPadding, 0, overlayPadding, 0)

        if (isOverlayTextAboveAnchor(anchorPoint)) {
            layout.doOnPreDraw { layout.y = anchorPoint.y - shapeHalfSize - overlayPadding - layout.height }
        } else {
            layout.y = anchorPoint.y + shapeHalfSize + overlayPadding
        }
    }

    private fun isOverlayTextAboveAnchor(anchorPoint: PointF): Boolean {
        val activity = (context as Activity)
        val insetsSystemBars = WindowInsetsCompat.Type.systemBars()
        val insetTop = ViewCompat.getRootWindowInsets(activity.window.decorView)?.getInsets(insetsSystemBars)?.top ?: 0
        val insetBottom = ViewCompat.getRootWindowInsets(activity.window.decorView)?.getInsets(insetsSystemBars)?.bottom ?: 0

        val windowMetrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity)
        val windowBounds = windowMetrics.bounds

        val availableHeight = windowBounds.height() - (insetTop + insetBottom)
        val anchorPointInWindow = PointF(anchorPoint.x, anchorPoint.y - insetTop)
        val areaAboveAnchor = anchorPointInWindow.y / availableHeight
        val areaBelowAnchor = (availableHeight - anchorPointInWindow.y) / availableHeight

        return areaAboveAnchor > areaBelowAnchor
    }
}