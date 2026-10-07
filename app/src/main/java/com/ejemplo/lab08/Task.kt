package com.ejemplo.lab08

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Priority { ALTA, MEDIA, BAJA }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val description: String,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIA,
    val category: String = "General",
    val isRecurring: Boolean = false,
    val recurrenceIntervalDays: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)