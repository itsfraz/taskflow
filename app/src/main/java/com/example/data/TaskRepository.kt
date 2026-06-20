package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class TaskRepository(
    private val context: Context,
    private val taskDao: TaskDao,
    val preferences: TaskPreferences
) {
    val allTasksFlow: Flow<List<TaskEntity>> = taskDao.getAllTasksFlow()
    val allCategoriesFlow: Flow<List<CategoryEntity>> = taskDao.getAllCategoriesFlow()

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)

    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?> = taskDao.getTaskByIdFlow(id)

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.deleteTask(task)

    suspend fun insertCategory(category: CategoryEntity): Long = taskDao.insertCategory(category)

    suspend fun deleteCategoryById(id: Long) = taskDao.deleteCategoryById(id)

    suspend fun updateTasksCompletionStatus(taskIds: List<Long>, isCompleted: Boolean) {
        taskDao.updateTasksCompletionStatus(taskIds, isCompleted, System.currentTimeMillis())
    }

    suspend fun deleteMultipleTasks(taskIds: List<Long>) {
        taskDao.deleteMultipleTasks(taskIds)
    }

    suspend fun deleteTasksByGroupId(groupId: String) {
        taskDao.deleteTasksByGroupId(groupId)
    }

    suspend fun getTasksByGroupId(groupId: String): List<TaskEntity> {
        return taskDao.getTasksByGroupId(groupId)
    }

    suspend fun deleteAllData() {
        taskDao.deleteAllTasks()
        preferences.clearAll()
    }

    // CSV LOGIC FOR EXPORTING & IMPORTING
    suspend fun exportToCsv(tasks: List<TaskEntity>): String {
        val stringBuilder = StringBuilder()
        // Header
        stringBuilder.append("ID,Title,Description,Category,Priority,DueDate,DueTime,ReminderTime,RepeatOption,ColorLabel,IsCompleted,IsPinned,IsFavorite,IsArchived\n")
        
        for (task in tasks) {
            val titleEscaped = escapeCsvValue(task.title)
            val descEscaped = escapeCsvValue(task.description)
            stringBuilder.append(
                "${task.id}," +
                "$titleEscaped," +
                "$descEscaped," +
                "${escapeCsvValue(task.category)}," +
                "${task.priority}," +
                "${task.dueDate ?: ""}," +
                "${task.dueTime ?: ""}," +
                "${task.reminderTime ?: ""}," +
                "${task.repeatOption}," +
                "${task.colorLabel}," +
                "${task.isCompleted}," +
                "${task.isPinned}," +
                "${task.isFavorite}," +
                "${task.isArchived}\n"
            )
        }
        return stringBuilder.toString()
    }

    suspend fun importFromCsv(csvContent: String): Int {
        val reader = InputStreamReader(csvContent.byteInputStream())
        val lines = reader.readLines()
        if (lines.isEmpty()) return 0

        var importedCount = 0
        // Skip header if it exists
        val startIdx = if (lines[0].startsWith("ID", ignoreCase = true)) 1 else 0

        for (i in startIdx until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            
            val tokens = parseCsvLine(line)
            if (tokens.size < 14) continue

            try {
                val title = tokens[1]
                val desc = tokens[2]
                val category = tokens[3]
                val priority = tokens[4]
                val dueDate = tokens[5].toLongOrNull()
                val dueTime = tokens[6].ifEmpty { null }
                val reminderTime = tokens[7].toLongOrNull()
                val repeatOption = tokens[8]
                val colorLabel = tokens[9]
                val isCompleted = tokens[10].toBoolean()
                val isPinned = tokens[11].toBoolean()
                val isFavorite = tokens[12].toBoolean()
                val isArchived = tokens[13].toBoolean()

                val task = TaskEntity(
                    title = title,
                    description = desc,
                    category = category,
                    priority = priority,
                    dueDate = dueDate,
                    dueTime = dueTime,
                    reminderTime = reminderTime,
                    repeatOption = repeatOption,
                    colorLabel = colorLabel,
                    isCompleted = isCompleted,
                    isPinned = isPinned,
                    isFavorite = isFavorite,
                    isArchived = isArchived,
                    createdDate = System.currentTimeMillis(),
                    updatedDate = System.currentTimeMillis()
                )
                taskDao.insertTask(task)
                importedCount++
            } catch (e: Exception) {
                // Ignore corrupt rows
            }
        }
        return importedCount
    }

    private fun escapeCsvValue(value: String): String {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        val curToken = StringBuilder()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    curToken.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(curToken.toString())
                curToken.clear()
            } else {
                curToken.append(c)
            }
            i++
        }
        tokens.add(curToken.toString())
        return tokens
    }

    // LOCAL BACKUP & RESTORE OF SQLite DB FILE
    fun backupDatabase(): Boolean {
        return try {
            val dbFile = context.getDatabasePath("task_flow_database")
            if (dbFile.exists()) {
                val backupFile = File(context.filesDir, "task_flow_database.backup")
                FileInputStream(dbFile).use { input ->
                    FileOutputStream(backupFile).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun restoreDatabase(): Boolean {
        return try {
            val backupFile = File(context.filesDir, "task_flow_database.backup")
            if (backupFile.exists()) {
                val dbFile = context.getDatabasePath("task_flow_database")
                
                // Close DB before restoring if needed. Since we are in VM, restarting/replacing on next app launch or resetting instance is fine.
                FileInputStream(backupFile).use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
