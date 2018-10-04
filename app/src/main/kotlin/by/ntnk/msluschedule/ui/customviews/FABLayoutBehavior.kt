package by.ntnk.msluschedule.ui.customviews

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import by.ntnk.msluschedule.R

@Suppress("UNUSED")
class FABLayoutBehavior(
        context: Context, attrs: AttributeSet
) : CoordinatorLayout.Behavior<RelativeLayout>(context, attrs) {
    private val famSlideDownAnim = AnimationUtils.loadAnimation(context, R.anim.fam_slide_down)
    private val famSlideUpAnim = AnimationUtils.loadAnimation(context, R.anim.fam_slide_up)

    override fun onStartNestedScroll(
            coordinatorLayout: CoordinatorLayout, child: RelativeLayout,
            directTargetChild: View, target: View, axes: Int, type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
        params.dodgeInsetEdges = Gravity.BOTTOM
    }

    override fun onNestedScroll(
            coordinatorLayout: CoordinatorLayout, child: RelativeLayout, target: View,
            dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int
    ) {
        if (child.visibility == View.VISIBLE && (dyConsumed > 0 || dyUnconsumed > 0)) {
            child.visibility = View.INVISIBLE
            child.startAnimation(famSlideDownAnim)
        } else if (child.visibility != View.VISIBLE && dyConsumed < 0) {
            child.visibility = View.VISIBLE
            child.startAnimation(famSlideUpAnim)
        }
    }
}
