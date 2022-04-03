package by.ntnk.msluschedule.ui.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import by.ntnk.msluschedule.R
import kotlin.math.ceil

abstract class ItemSwipeCallback(context: Context) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {
    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_note) as Drawable
    private val background = ColorDrawable()
    private val backgroundColor = ContextCompat.getColor(context, R.color.destructive_action)
    private val deleteIconHorizontalMargin =
        context.resources.getDimension(R.dimen.item_note_deleteicon_margin_right).toInt()

    override fun onMove(
        recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onChildDraw(
        c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        background.color = backgroundColor
        if (dX.toInt() < 0) {
            background.setBounds(itemView.right + ceil(dX).toInt(), itemView.top, itemView.right, itemView.bottom)
        } else {
            background.setBounds(itemView.left, itemView.top, itemView.left + ceil(dX).toInt(), itemView.bottom)
        }
        background.draw(c)

        val deleteIconTop = itemView.top + (itemView.bottom - itemView.top - deleteIcon.intrinsicHeight) / 2
        val deleteIconLeft = if (dX.toInt() < 0) {
            itemView.right - deleteIconHorizontalMargin - deleteIcon.intrinsicWidth
        } else {
            itemView.left + deleteIconHorizontalMargin
        }
        val deleteIconRight = if (dX.toInt() < 0) {
            itemView.right - deleteIconHorizontalMargin
        } else {
            itemView.left + deleteIconHorizontalMargin + deleteIcon.intrinsicWidth
        }
        val deleteIconBottom = deleteIconTop + deleteIcon.intrinsicHeight
        deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
        deleteIcon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }

    companion object {
        /**
         * No direction, used for swipe & drag control.
         */
        const val NO_DIRECION = 0
    }
}