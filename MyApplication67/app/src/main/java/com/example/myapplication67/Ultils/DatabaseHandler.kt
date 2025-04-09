package com.example.myapplication67.Ultils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.myapplication67.Model.ToDoModel

// This class manages local database operations using SQLiteOpenHelper
class DatabaseHandler(context: Context) :
    SQLiteOpenHelper(context, NAME, null, VERSION) {

    private var db: SQLiteDatabase? = null // Reference to writable database

    // Called only once when the database is created for the first time
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TODO_TABLE)
    }

    // Called when database version is upgraded
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Add due date column if upgrading from version < 2
            db.execSQL("ALTER TABLE $TODO_TABLE ADD COLUMN $DUE_DATE LONG DEFAULT 0")
        }
    }

    // Open the database connection for reading/writing
    fun openDatabase() {
        db = this.writableDatabase
    }

    // Insert a new task into the database
    fun insertTask(task: ToDoModel): Long {
        val localDb = db ?: return -1
        val cv = ContentValues().apply {
            put(TASK, task.task)
            put(STATUS, task.status)
            put(DUE_DATE, task.dueDate)
        }
        return localDb.insert(TODO_TABLE, null, cv)
    }

    // Get all tasks from the database, sorted by newest first
    fun getAllTasks(): List<ToDoModel> {
        val taskList = mutableListOf<ToDoModel>()
        val localDb = db ?: return taskList

        val cursor: Cursor? = localDb.query(
            TODO_TABLE,
            null,      // Get all columns
            null,      // No WHERE clause
            null,      // No selection args
            null,
            null,
            "$ID DESC" // Order by newest first
        )

        // Read each row and convert to ToDoModel
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val task = ToDoModel().apply {
                        id = it.getInt(it.getColumnIndexOrThrow(ID))
                        task = it.getString(it.getColumnIndexOrThrow(TASK))
                        status = it.getInt(it.getColumnIndexOrThrow(STATUS))
                        dueDate = try {
                            it.getLong(it.getColumnIndexOrThrow(DUE_DATE))
                        } catch (e: Exception) {
                            0L // Default fallback if column is missing
                        }
                    }
                    taskList.add(task)
                } while (it.moveToNext())
            }
        }
        return taskList
    }

    // Update only the completion status of a task
    fun updateStatus(id: Int, status: Int) {
        val localDb = db ?: return
        val cv = ContentValues().apply {
            put(STATUS, status)
        }
        localDb.update(TODO_TABLE, cv, "$ID = ?", arrayOf(id.toString()))
    }

    // Update task text and optionally due date
    fun updateTask(id: Int, task: String, dueDate: Long = 0) {
        val localDb = db ?: return
        val cv = ContentValues().apply {
            put(TASK, task)
            put(DUE_DATE, dueDate)
        }
        localDb.update(TODO_TABLE, cv, "$ID = ?", arrayOf(id.toString()))
    }

    // Delete a task from the database
    fun deleteTask(id: Int) {
        val localDb = db ?: return
        localDb.delete(TODO_TABLE, "$ID = ?", arrayOf(id.toString()))
    }

    companion object {
        private const val VERSION = 2                  // Current DB version
        const val NAME = "toDoListDatabase"            // DB name
        private const val TODO_TABLE = "todo"          // Table name
        private const val ID = "id"                    // Task ID column
        private const val TASK = "task"                // Task name column
        private const val STATUS = "status"            // 0 = incomplete, 1 = complete
        private const val DUE_DATE = "due_date"        // Time in millis

        // SQL command to create table with ID, task text, status, and due date
        private const val CREATE_TODO_TABLE =
            "CREATE TABLE $TODO_TABLE($ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$TASK TEXT, $STATUS INTEGER, $DUE_DATE LONG DEFAULT 0)"
    }
}
