package com.example.myapplication67

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import com.example.myapplication67.Model.ToDoModel
import com.example.myapplication67.Ultils.DatabaseHandler
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.*

// Bottom sheet dialog to create or edit a task with date & time selection
class AddNewTask : BottomSheetDialogFragment() {

    private lateinit var newTaskText: EditText
    private lateinit var newTaskSaveButton: Button
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var db: DatabaseHandler

    private var selectedDate: Calendar = Calendar.getInstance() // Holds chosen date & time
    private var isDateSet = false
    private var isTimeSet = false
    private var listener: OnTaskAddedListener? = null

    companion object {
        const val TAG = "ActionBottomDialog"
        fun newInstance(): AddNewTask = AddNewTask()
    }

    // Callback interface to notify host activity/fragment when a task is added or canceled
    interface OnTaskAddedListener {
        fun onTaskAdded()
        fun onTaskAddCancelled()
    }

    fun setOnTaskAddedListener(callback: OnTaskAddedListener) {
        listener = callback
    }

    // Inflate either edit or new task layout based on arguments
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val isEditingLayout = arguments?.getBoolean("isEditingLayout", false) ?: false
        val layoutId = if (isEditingLayout) R.layout.edit_task else R.layout.new_task
        return inflater.inflate(layoutId, container, false)
    }

    // Setup UI and handle button clicks
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var isUpdate = false
        val bundle = arguments

        newTaskText = view.findViewById(R.id.newTaskText)
        newTaskSaveButton = view.findViewById(R.id.NewTaskButton)
        dateButton = view.findViewById(R.id.dateButton)
        timeButton = view.findViewById(R.id.timeButton)

        db = DatabaseHandler(requireActivity())
        db.openDatabase()

        // Open date/time picker on button click
        dateButton.setOnClickListener { showDatePicker() }
        timeButton.setOnClickListener { showTimePicker() }

        // If this is an edit operation, pre-fill task data
        if (bundle != null && bundle.containsKey("task")) {
            isUpdate = true
            val task = bundle.getString("task")
            newTaskText.setText(task)
            if (!task.isNullOrEmpty()) {
                newTaskSaveButton.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
                )
            }
        }

        // Handle save button click
        newTaskSaveButton.setOnClickListener {
            val text = newTaskText.text.toString().trim()

            // Require text input
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Please add the task", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Require date and time selection
            if (!isDateSet || !isTimeSet) {
                Toast.makeText(requireContext(), "Please select both date and time.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isUpdate) {
                // Update task in DB
                db.updateTask(bundle!!.getInt("id"), text, selectedDate.timeInMillis)
                listener?.onTaskAdded()
            } else {
                // Create new task in DB
                val task = ToDoModel().apply {
                    this.task = text
                    this.status = 0
                    this.dueDate = selectedDate.timeInMillis
                }
                val id = db.insertTask(task).toInt()
                task.id = id

                // Schedule alarm/notification
                setAlarm(task)

                Toast.makeText(requireContext(), "Task added", Toast.LENGTH_SHORT).show()
                listener?.onTaskAdded()
            }

            // Notify MainActivity and dismiss dialog
            (activity as? MainActivity)?.handleDialogClose(dialog!!)
            dismiss()
        }
    }

    // Notify listener if dialog was canceled (e.g., via swipe down)
    override fun onCancel(dialog: android.content.DialogInterface) {
        super.onCancel(dialog)
        listener?.onTaskAddCancelled()
    }

    // Launch date picker dialog and set result
    private fun showDatePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now()) // Prevent past dates

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Due Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedDate.timeInMillis = selection
            isDateSet = true
            dateButton.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selection))
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    // Launch time picker dialog and set result
    private fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setTitleText("Select Due Time")
            .setHour(12)
            .setMinute(0)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            selectedDate.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            selectedDate.set(Calendar.MINUTE, timePicker.minute)
            isTimeSet = true
            timeButton.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(selectedDate.time)
        }

        timePicker.show(parentFragmentManager, "TIME_PICKER")
    }

    // Schedule an alarm for the task using AlarmManager
    private fun setAlarm(task: ToDoModel) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
            putExtra("task", task.task)
            putExtra("id", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, task.dueDate, pendingIntent)
    }
}
