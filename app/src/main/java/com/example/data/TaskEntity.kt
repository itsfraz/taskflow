package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // Matches CategoryEntity.name
    val priority: String, // Low, Medium, High, Urgent
    val dueDate: Long?,   // Epoch milliseconds
    val dueTime: String?, // Format: "HH:mm"
    val reminderTime: Long?, // Epoch milliseconds to trigger reminder
    val repeatOption: String, // None, Daily, Weekly, Monthly
    val colorLabel: String, // hex color code
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val scheduleGroupId: String? = null,
    val scheduleConfig: String? = null,
    val endTime: String? = null, // Format: "HH:mm"
    val estimatedDuration: Int = 30 // Duration in minutes
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String, // Match icon names like "work", "school", etc.
    val colorHex: String,
    val isSystem: Boolean = false
)
