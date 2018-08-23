package by.ntnk.msluschedule.ui.customviews

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import by.ntnk.msluschedule.R

@Suppress("UNUSED")
class FABLayoutBehavior(
        context: Context, attrs: AttributeSet
) : CoordinatorLayout.Behavior<LinearLayout>(context, attrs) {
    override fun onStartNestedScroll(
            coordinatorLayout: CoordinatorLayout, child: LinearLayout,
            directTargetChild: View, target: View, axes: Int, type: Int
    ): Boolean {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
    }

    override fun onAttachedToLayoutParams(params: CoordinatorLayout.LayoutParams) {
        super.onAttachedToLayoutParams(params)
        params.dodgeInsetEdges = Gravity.BOTTOM
    }

    override fun onNestedScroll(
            coordinatorLayout: CoordinatorLayout, child: LinearLayout, target: View,
            dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int
    ) {
        if (child.visibility == View.VISIBLE && dyConsumed > 0) {
            child.visibility = View.INVISIBLE
            child.startAnimation(AnimationUtils.loadAnimation(child.context, R.anim.fam_slide_down))

        } else if (child.visibility == View.INVISIBLE && dyConsumed < 0) {
            child.visibility = View.VISIBLE
            child.startAnimation(AnimationUtils.loadAnimation(child.context, R.anim.fam_slide_up))
        }
    }
}
