package com.example.myapplication67.Model

data class ToDoModel(
    var id: Int = 0,
    var status: Int = 0,
    var task: String = "",
    var dueDate: Long = 0
)