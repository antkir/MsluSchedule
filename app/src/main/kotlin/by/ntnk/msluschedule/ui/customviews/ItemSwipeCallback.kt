package by.ntnk.msluschedule.ui.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import by.ntnk.msluschedule.R

abstract class ItemSwipeCallback(context: Context) :
        ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete_note) as Drawable
    private val background = ColorDrawable()
    private val backgroundColor = ContextCompat.getColor(context, R.color.warning)
    private val deleteIconRightMargin =
            context.resources.getDimension(R.dimen.item_note_deleteicon_margin_right).toInt()

    override fun onMove(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?,
                        target: RecyclerView.ViewHolder?): Boolean {
        return false
    }

    override fun onChildDraw(c: Canvas?, recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder,
                             dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        val itemView = viewHolder.itemView
        background.color = backgroundColor
        background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
        background.draw(c)

        val deleteIconTop = itemView.top + (itemView.bottom - itemView.top - deleteIcon.intrinsicHeight) / 2
        val deleteIconLeft = itemView.right - deleteIconRightMargin - deleteIcon.intrinsicWidth
        val deleteIconRight = itemView.right - deleteIconRightMargin
        val deleteIconBottom = deleteIconTop + deleteIcon.intrinsicHeight
        deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom)
        deleteIcon.draw(c)

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}