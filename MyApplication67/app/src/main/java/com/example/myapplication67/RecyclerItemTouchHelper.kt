package com.example.myapplication67

import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication67.Adapter.ToDoAdapter
import com.example.myapplication67.Model.ToDoModel

// This class enables swipe-to-edit and swipe-to-delete functionality in the RecyclerView.
class RecyclerItemTouchHelper(private val adapter: ToDoAdapter) :
    ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    // Keeps track of which item is being edited after a swipe right
    private var pendingDeletePosition = RecyclerView.NO_POSITION

    // Drag-and-drop is disabled by returning false here
    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, tgt: RecyclerView.ViewHolder): Boolean = false

    // Return 1.0f to prevent default swipe dismissal behavior
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 1.0f
    }

    // Called when user swipes an item left or right
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        if (position == RecyclerView.NO_POSITION) return

        val task = adapter.getItem(position)

        // Cancel swipe if it's a header (not a task)
        if (task == null) {
            adapter.notifyItemChanged(position)
            return
        }

        when (direction) {
            ItemTouchHelper.LEFT -> {
                // Left swipe triggers delete confirmation dialog
                showDeleteConfirmation(position)
            }
            ItemTouchHelper.RIGHT -> {
                // Right swipe triggers edit dialog
                pendingDeletePosition = position

                // Reset item position visually
                viewHolder.itemView.animate().translationX(0f).setDuration(100).start()

                // Delay opening dialog to allow animation to complete
                Handler(Looper.getMainLooper()).postDelayed({
                    showEditTaskDialog(task)
                }, 150)
            }
        }
    }

    // Shows edit dialog when task is swiped right
    private fun showEditTaskDialog(task: ToDoModel) {
        val dialog = AddNewTask.newInstance().apply {
            arguments = Bundle().apply {
                putBoolean("isEditingLayout", true)
                putInt("id", task.id)
                putString("task", task.task)
                putLong("dueDate", task.dueDate)
            }
        }

        // Handle callback after editing
        dialog.setOnTaskAddedListener(object : AddNewTask.OnTaskAddedListener {
            override fun onTaskAdded() {
                Toast.makeText(adapter.activity, "Task updated", Toast.LENGTH_SHORT).show()
                // Reset position if necessary
                if (pendingDeletePosition != RecyclerView.NO_POSITION) {
                    adapter.notifyItemChanged(pendingDeletePosition)
                    pendingDeletePosition = RecyclerView.NO_POSITION
                }
            }

            override fun onTaskAddCancelled() {
                // If canceled, reset item to original state
                if (pendingDeletePosition != RecyclerView.NO_POSITION) {
                    adapter.notifyItemChanged(pendingDeletePosition)
                    pendingDeletePosition = RecyclerView.NO_POSITION
                }
            }
        })

        dialog.show(adapter.activity.supportFragmentManager, AddNewTask.TAG)
    }

    // Show a confirmation dialog when swiping left to delete
    private fun showDeleteConfirmation(position: Int) {
        val builder = AlertDialog.Builder(adapter.activity)
        builder.setTitle("Delete Task")
        builder.setMessage("Are you sure you want to delete this Task?")
        builder.setPositiveButton("Confirm") { _, _ ->
            adapter.deleteItem(position)
            Toast.makeText(adapter.activity, "Task deleted", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
            adapter.notifyItemChanged(position)
            dialog.dismiss()
        }
        builder.setOnCancelListener {
            adapter.notifyItemChanged(position)
        }
        builder.create().show()
    }

    // Custom drawing during swipe gesture (show background and icons)
    override fun onChildDraw(
        c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        val itemView = vh.itemView
        val icon: Drawable?
        val background: ColorDrawable
        val offset = 20 // To avoid overlap

        // Determine swipe direction and select appropriate icon and background
        if (dX > 0) {
            icon = ContextCompat.getDrawable(adapter.activity, R.drawable.edit)
            background = ColorDrawable(ContextCompat.getColor(adapter.activity, R.color.colorPrimary))
        } else {
            icon = ContextCompat.getDrawable(adapter.activity, R.drawable.trash)
            background = ColorDrawable(Color.RED)
        }

        icon?.let {
            // Calculate vertical icon positioning
            val iconMargin = (itemView.height - it.intrinsicHeight) / 2
            val iconTop = itemView.top + iconMargin
            val iconBottom = iconTop + it.intrinsicHeight

            if (dX > 0) {
                // Right swipe (edit)
                val iconLeft = itemView.left + iconMargin
                val iconRight = iconLeft + it.intrinsicWidth
                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt() + offset, itemView.bottom)
            } else {
                // Left swipe (delete)
                val iconRight = itemView.right - iconMargin
                val iconLeft = iconRight - it.intrinsicWidth
                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                background.setBounds(itemView.right + dX.toInt() - offset, itemView.top, itemView.right, itemView.bottom)
            }

            // Draw the background and icon on the canvas
            background.draw(c)
            it.draw(c)
        }

        // Call superclass to finish the drawing
        super.onChildDraw(c, rv, vh, dX, dY, actionState, isCurrentlyActive)
    }
}
