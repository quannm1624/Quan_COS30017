package com.example.myapplication67

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication67.Adapter.ToDoAdapter
import com.example.myapplication67.Model.ToDoModel
import com.example.myapplication67.Ultils.DatabaseHandler
import com.google.android.material.floatingactionbutton.FloatingActionButton

// Main screen of the app: handles UI, task display, permissions, and alarms
class MainActivity : AppCompatActivity(), DialogCloseListener {

    private lateinit var db: DatabaseHandler              // Reference to database handler
    private lateinit var tasksRecyclerView: RecyclerView  // RecyclerView for task list
    private lateinit var tasksAdapter: ToDoAdapter        // Adapter to manage list items
    private lateinit var fab: FloatingActionButton        // Floating action button to add tasks
    private var taskList: MutableList<ToDoModel> = mutableListOf() // List of tasks

    // Permission launcher for Android 13+ notifications
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notification permission is required for task reminders",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        // Initialize everything
        initDatabase()
        initViews()
        setupRecyclerView()
        loadTasks()
        checkNotificationPermission()
        checkExactAlarmPermission()
    }

    // Safely initialize and open the local database
    private fun initDatabase() {
        try {
            db = DatabaseHandler(this)
            db.openDatabase()
            val testTasks = db.getAllTasks() // Test read
            Log.d("MainActivity", "Database initialized with ${testTasks.size} tasks")
        } catch (e: Exception) {
            Log.e("MainActivity", "Database initialization failed", e)
            try {
                // If something breaks, reset database
                this.deleteDatabase(DatabaseHandler.NAME)
                db = DatabaseHandler(this)
                db.openDatabase()
                Toast.makeText(this, "Database reset successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to reset database", e)
                Toast.makeText(this, "Critical database error. Please reinstall the app.", Toast.LENGTH_LONG).show()
                finish() // Exit app
            }
        }
    }

    // Find UI components and set up click listeners
    private fun initViews() {
        tasksRecyclerView = findViewById(R.id.taskrecyclerView)
        fab = findViewById(R.id.fab)

        // Open dialog to add a new task
        fab.setOnClickListener {
            AddNewTask.newInstance().show(supportFragmentManager, AddNewTask.TAG)
        }
    }

    // Prepare RecyclerView with adapter and swipe-to-delete functionality
    private fun setupRecyclerView() {
        tasksAdapter = ToDoAdapter(db, this)
        tasksRecyclerView.layoutManager = LinearLayoutManager(this)
        tasksRecyclerView.adapter = tasksAdapter

        // Enable swipe gestures on tasks
        val itemTouchHelper = ItemTouchHelper(RecyclerItemTouchHelper(tasksAdapter))
        itemTouchHelper.attachToRecyclerView(tasksRecyclerView)
    }

    // Load tasks from database and display them
    private fun loadTasks() {
        try {
            taskList = db.getAllTasks().toMutableList()
            taskList.reverse() // Show latest tasks on top
            tasksAdapter.setTasks(taskList)
            scheduleAllActiveAlarms()
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading tasks", Toast.LENGTH_SHORT).show()
            taskList = mutableListOf()
            tasksAdapter.setTasks(taskList)
        }
    }

    // Request notification permission (Android 13+)
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(
                        this,
                        "Notification permission is required for task reminders",
                        Toast.LENGTH_LONG
                    ).show()
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    // Check and warn if exact alarm permission is not granted (Android 12+)
    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(
                    this,
                    "Exact alarm permission is required for accurate task reminders. Please enable in app settings.",
                    Toast.LENGTH_LONG
                ).show()
                // You can navigate to app settings if needed
                // val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                // startActivity(intent)
            }
        }
    }

    // Schedule alarms for all upcoming tasks
    private fun scheduleAllActiveAlarms() {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            taskList.forEach { task ->
                if (task.dueDate > System.currentTimeMillis() && task.status == 0) {
                    val intent = Intent(this, AlarmReceiver::class.java).apply {
                        putExtra("task", task.task)
                        putExtra("id", task.id)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        this,
                        task.id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Use different scheduling depending on Android version
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (alarmManager.canScheduleExactAlarms()) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                task.dueDate,
                                pendingIntent
                            )
                        }
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            task.dueDate,
                            pendingIntent
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error scheduling alarms", Toast.LENGTH_SHORT).show()
        }
    }

    // Called when the Add/Edit dialog is closed
    override fun handleDialogClose(dialog: DialogInterface) {
        try {
            taskList = db.getAllTasks().toMutableList()
            taskList.reverse()
            tasksAdapter.setTasks(taskList)
            tasksAdapter.notifyDataSetChanged()
            scheduleAllActiveAlarms() // Reschedule in case due date changed
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating tasks", Toast.LENGTH_SHORT).show()
        }
    }

    // Clean up resources on destroy
    override fun onDestroy() {
        try {
            db.close()
        } catch (e: Exception) {
            // Optional: log error
        }
        super.onDestroy()
    }
}
