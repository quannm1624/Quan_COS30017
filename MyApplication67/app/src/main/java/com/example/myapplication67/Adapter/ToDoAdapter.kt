package com.example.myapplication67.Adapter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication67.*
import com.example.myapplication67.Model.ToDoModel
import com.example.myapplication67.Ultils.DatabaseHandler
import java.text.SimpleDateFormat
import java.util.*

class ToDoAdapter(private val db: DatabaseHandler, val activity: MainActivity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_HEADER = 0
    private val VIEW_TYPE_ITEM = 1

    // Display list can hold either section headers (String) or tasks (ToDoModel)
    private var displayList: MutableList<Any> = mutableListOf()

    // Get task at a specific position (returns null if it's a section header)
    fun getItem(position: Int): ToDoModel? {
        val item = displayList[position]
        return if (item is ToDoModel) item else null
    }

    // Sets the tasks and organizes them into sectioned headers
    fun setTasks(tasks: List<ToDoModel>) {
        displayList.clear()

        // Setup time boundaries
        val now = Calendar.getInstance()
        val todayStart = now.clone() as Calendar
        todayStart.set(Calendar.HOUR_OF_DAY, 0)
        todayStart.set(Calendar.MINUTE, 0)
        todayStart.set(Calendar.SECOND, 0)
        todayStart.set(Calendar.MILLISECOND, 0)

        val todayMillis = todayStart.timeInMillis
        val tomorrowMillis = todayMillis + 24 * 60 * 60 * 1000
        val nextWeekMillis = todayMillis + 7 * 24 * 60 * 60 * 1000

        // Map tasks to section labels
        val sections = mutableMapOf<String, MutableList<ToDoModel>>()

        for (task in tasks.sortedBy { it.dueDate }) {
            val section = when {
                task.dueDate <= 0 -> "Undated"
                task.dueDate < todayMillis -> "Past"
                task.dueDate < tomorrowMillis -> "Today"
                task.dueDate < tomorrowMillis + 24 * 60 * 60 * 1000 -> "Tomorrow"
                task.dueDate < nextWeekMillis -> {
                    val daysLeft = ((task.dueDate - todayMillis) / (1000 * 60 * 60 * 24)).toInt()
                    "Next $daysLeft days"
                }
                else -> {
                    val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                    sdf.format(Date(task.dueDate))
                }
            }
            sections.getOrPut(section) { mutableListOf() }.add(task)
        }

        // Populate display list with headers and items
        for ((section, items) in sections) {
            displayList.add(section)
            displayList.addAll(items)
        }

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = displayList.size

    override fun getItemViewType(position: Int): Int {
        return if (displayList[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_section_header, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.task_layout, parent, false)
            TaskViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            // Bind section header text
            holder.sectionHeader.text = displayList[position] as String
        } else if (holder is TaskViewHolder) {
            val task = displayList[position] as ToDoModel
            db.openDatabase()

            holder.task.text = task.task

            // Prevent unwanted listener triggering during checkbox reset
            holder.task.setOnCheckedChangeListener(null)
            holder.task.isChecked = task.status != 0

            // Apply or remove strikethrough for completed tasks
            if (task.status != 0) {
                holder.task.paintFlags = holder.task.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.task.paintFlags = holder.task.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            // Handle checkbox toggle logic
            holder.task.setOnCheckedChangeListener { _, isChecked ->
                val newStatus = if (isChecked) 1 else 0
                task.status = newStatus
                db.updateStatus(task.id, newStatus)

                if (isChecked) cancelAlarm(task)
                else if (task.dueDate > 0) setAlarm(task)

                notifyItemChanged(position)
            }

            // Show due date if available
            val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            if (task.dueDate > 0) {
                val calendar = Calendar.getInstance().apply { timeInMillis = task.dueDate }
                val hasTimeComponent = calendar.get(Calendar.HOUR_OF_DAY) != 0 || calendar.get(Calendar.MINUTE) != 0

                if (hasTimeComponent) {
                    holder.dueDateText.text = activity.getString(
                        R.string.due_date_format,
                        dateFormat.format(Date(task.dueDate))
                    )
                } else {
                    holder.dueDateText.text = activity.getString(R.string.undated_label)
                    Toast.makeText(
                        activity,
                        "Please select both date and time for the task.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                holder.dueDateText.text = activity.getString(R.string.undated_label)
            }

            // Enable long click to open edit dialog
            holder.itemView.setOnLongClickListener {
                editItem(position)
                true
            }
        }
    }

    // ViewHolder for tasks
    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val task: CheckBox = view.findViewById(R.id.todoCheckbox)
        val dueDateText: TextView = view.findViewById(R.id.dueDateText)
    }

    // ViewHolder for section headers
    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sectionHeader: TextView = view.findViewById(R.id.sectionHeader)
    }

    // Delete task from database and UI, remove section header if empty
    fun deleteItem(position: Int) {
        val item = displayList[position]
        if (item is ToDoModel) {
            db.deleteTask(item.id)
            cancelAlarm(item)
            displayList.removeAt(position)

            // Check if its section is now empty
            val sectionIndex = findSectionHeaderIndexBefore(position)
            var sectionIsEmpty = true
            if (sectionIndex != -1) {
                var i = sectionIndex + 1
                while (i < displayList.size && displayList[i] !is String) {
                    sectionIsEmpty = false
                    break
                }
            }

            if (sectionIsEmpty && sectionIndex != -1) {
                displayList.removeAt(sectionIndex)
                notifyItemRemoved(sectionIndex)
                notifyItemRemoved(position - 1)
            } else {
                notifyItemRemoved(position)
            }
        }
    }

    // Finds the index of the section header just before a given position
    private fun findSectionHeaderIndexBefore(position: Int): Int {
        for (i in position - 1 downTo 0) {
            if (displayList[i] is String) return i
        }
        return -1
    }

    // Launch the AddNewTask fragment to edit the selected item
    fun editItem(position: Int) {
        val item = displayList[position]
        if (item is ToDoModel) {
            val bundle = Bundle().apply {
                putInt("id", item.id)
                putString("task", item.task)
                if (item.dueDate > 0) putLong("dueDate", item.dueDate)
            }

            val dialog = AddNewTask().apply {
                arguments = bundle
            }

            dialog.show(activity.supportFragmentManager, AddNewTask.TAG)
        }
    }

    // Set alarm for a task if dueDate is valid and task is not done
    private fun setAlarm(task: ToDoModel) {
        if (task.dueDate <= System.currentTimeMillis() || task.status != 0) return

        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(activity, AlarmReceiver::class.java).apply {
            putExtra("task", task.task)
            putExtra("id", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            activity,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, task.dueDate, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.dueDate, pendingIntent)
        }
    }

    // Cancel any previously set alarm for a task
    private fun cancelAlarm(task: ToDoModel) {
        if (task.dueDate <= 0) return

        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(activity, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            activity,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }
}
