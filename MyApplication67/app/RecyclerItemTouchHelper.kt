 package com.example.myapplication3

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication3.Adapter.ToDoAdapter

class RecyclerItemTouchHelper(private val adapter: ToDoAdapter) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {


    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (direction == ItemTouchHelper.LEFT) {
            val builder = AlertDialog.Builder(adapter.activity) // Use activity reference for context
            builder.setTitle("Delete Task")
            builder.setMessage("Are you sure you want to delete this Task?")
            builder.setPositiveButton("Confirm") { _, _ ->
                adapter.deleteItem(position)
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                adapter.notifyItemChanged(viewHolder.adapterPosition)
            }
            builder.create().show()
        } else {
            adapter.editItem(position)
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

        val itemView = viewHolder.itemView
        val backgroundCornerOffset = 20

        val icon: Drawable?
        val background: ColorDrawable

        if (dX > 0) {
            icon = ContextCompat.getDrawable(adapter.activity, R.drawable.edit) // Use activity reference
            background = ColorDrawable(ContextCompat.getColor(adapter.activity, R.color.colorPrimaryDark)) // Use activity reference
        } else {
            icon = ContextCompat.getDrawable(adapter.activity, R.drawable.trash) // Use activity reference
            background = ColorDrawable(Color.RED)
        }

        icon?.let {
            val iconMargin = (itemView.height - it.intrinsicHeight) / 2
            val iconTop = itemView.top + iconMargin
            val iconBottom = iconTop + it.intrinsicHeight

            if (dX > 0) {
                val iconLeft = itemView.left + iconMargin
                val iconRight = itemView.left + iconMargin + it.intrinsicWidth
                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                background.setBounds(
                    itemView.left, itemView.top,
                    itemView.left + dX.toInt() + backgroundCornerOffset, itemView.bottom
                )
            } else if (dX < 0) {
                val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                val iconRight = itemView.right - iconMargin
                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                background.setBounds(
                    itemView.right + dX.toInt() - backgroundCornerOffset,
                    itemView.top, itemView.right, itemView.bottom
                )
            } else {
                background.setBounds(0, 0, 0, 0)
            }

            background.draw(c)
            it.draw(c)
        }
    }
}
