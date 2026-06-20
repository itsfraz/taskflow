package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CategoryEntity
import com.example.data.TaskEntity
import com.example.data.TaskRepository
import com.example.worker.ReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    // RAW SOURCE FLOWS
    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategoriesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // SETTINGS PREFERENCES
    val themeState: StateFlow<String> = repository.preferences.themeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "System"
    )

    val notificationsEnabled: StateFlow<Boolean> = repository.preferences.notificationsEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val fontSizeScale: StateFlow<Float> = repository.preferences.fontSizeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0f
    )

    val selectedLanguage: StateFlow<String> = repository.preferences.languageFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "en"
    )

    // INTERACTIVE FILTERS & STATES
    val searchQuery = MutableStateFlow("")
    val filterCategory = MutableStateFlow<String?>(null) // null = All
    val filterPriority = MutableStateFlow<String?>(null) // null = All
    val filterTimeframe = MutableStateFlow("Today") // All, Today, Tomorrow, This Week, Overdue, Archived

    // SORT OPTIONS
    // Options: "Newest First", "Oldest First", "A-Z", "Due Date", "Priority", "Completed Last", "Pending First"
    val sortOption = MutableStateFlow("Newest First")

    // BULK SELECTION
    val selectedTaskIds = MutableStateFlow<Set<Long>>(emptySet())

    data class FilterState(
        val category: String?,
        val priority: String?,
        val timeframe: String,
        val sort: String
    )

    // FILTERED/SORTED TASKS COMBINED FLOW
    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        searchQuery,
        combine(filterCategory, filterPriority, filterTimeframe, sortOption) { cat, prio, timeframe, sort ->
            FilterState(cat, prio, timeframe, sort)
        }
    ) { tasks, query, filter ->
        var result = tasks

        val cat = filter.category
        val prio = filter.priority
        val timeframe = filter.timeframe
        val sort = filter.sort

        // 1. Search Query
        if (query.isNotEmpty()) {
            result = result.filter { task ->
                task.title.contains(query, ignoreCase = true) ||
                task.description.contains(query, ignoreCase = true) ||
                task.category.contains(query, ignoreCase = true)
            }
        }

        // 2. Filter Category
        if (cat != null) {
            result = result.filter { it.category == cat }
        }

        // 3. Filter Priority
        if (prio != null) {
            result = result.filter { it.priority.lowercase() == prio.lowercase() }
        }

        // 4. Filter Timeframe
        val calendar = Calendar.getInstance()
        val todayStart = calendar.clone() as Calendar
        todayStart.set(Calendar.HOUR_OF_DAY, 0)
        todayStart.set(Calendar.MINUTE, 0)
        todayStart.set(Calendar.SECOND, 0)
        todayStart.set(Calendar.MILLISECOND, 0)

        val todayEnd = todayStart.clone() as Calendar
        todayEnd.add(Calendar.DAY_OF_YEAR, 1)

        val tomorrowEnd = todayEnd.clone() as Calendar
        tomorrowEnd.add(Calendar.DAY_OF_YEAR, 1)

        val thisWeekEnd = todayStart.clone() as Calendar
        thisWeekEnd.add(Calendar.DAY_OF_YEAR, 7)

        result = when (timeframe) {
            "Today" -> {
                result.filter { task ->
                    task.dueDate != null && 
                    task.dueDate >= todayStart.timeInMillis && 
                    task.dueDate < todayEnd.timeInMillis &&
                    !task.isArchived
                }
            }
            "Tomorrow" -> {
                result.filter { task ->
                    task.dueDate != null && 
                    task.dueDate >= todayEnd.timeInMillis && 
                    task.dueDate < tomorrowEnd.timeInMillis &&
                    !task.isArchived
                }
            }
            "This Week" -> {
                result.filter { task ->
                    task.dueDate != null && 
                    task.dueDate >= todayStart.timeInMillis && 
                    task.dueDate < thisWeekEnd.timeInMillis &&
                    !task.isArchived
                }
            }
            "Upcoming" -> {
                result.filter { task ->
                    task.dueDate != null && 
                    task.dueDate >= todayEnd.timeInMillis && 
                    !task.isArchived
                }
            }
            "Completed" -> {
                result.filter { task ->
                    task.isCompleted && 
                    !task.isArchived
                }
            }
            "Overdue" -> {
                result.filter { task ->
                    task.dueDate != null && 
                    task.dueDate < todayStart.timeInMillis && 
                    !task.isCompleted &&
                    !task.isArchived
                }
            }
            "Archived" -> {
                result.filter { it.isArchived }
            }
            "Active" -> {
                result.filter { !it.isArchived }
            }
            else -> { // "All"
                result.filter { !it.isArchived }
            }
        }

        // 5. Sorting
        result = when (sort) {
            "Oldest First" -> result.sortedBy { it.createdDate }
            "A-Z" -> result.sortedBy { it.title.lowercase() }
            "Due Date" -> result.sortedWith(compareBy<TaskEntity> { it.dueDate ?: Long.MAX_VALUE }.thenBy { it.dueTime ?: "23:59" })
            "Priority" -> {
                val priorityOrder = mapOf("urgent" to 4, "high" to 3, "medium" to 2, "low" to 1)
                result.sortedByDescending { priorityOrder[it.priority.lowercase()] ?: 0 }
            }
            "Completed Last" -> result.sortedBy { it.isCompleted }
            "Pending First" -> result.sortedByDescending { !it.isCompleted }
            else -> result.sortedByDescending { it.createdDate } // Newest First
        }

        result
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // REPOSITORY WRITE ACTIONS wrapper
    fun insertTask(
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDate: Long?,
        dueTime: String?,
        reminderOffsetMin: Int?, // Offset in minutes or null
        colorLabel: String,
        repeatOption: String = "None"
    ): String? {
        // Complete Input Validation
        if (title.trim().isEmpty()) {
            return "Task title cannot be empty"
        }

        var reminderTimeMs: Long? = null
        if (dueDate != null && reminderOffsetMin != null && reminderOffsetMin > 0) {
            // Compute reminder trigger time based on due date and (optional) time
            val cal = Calendar.getInstance().apply {
                timeInMillis = dueDate
                if (dueTime != null) {
                    val timeParts = dueTime.split(":")
                    if (timeParts.size == 2) {
                        set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 12)
                        set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
                    }
                }
            }
            cal.add(Calendar.MINUTE, -reminderOffsetMin)
            reminderTimeMs = cal.timeInMillis

            if (reminderTimeMs < System.currentTimeMillis()) {
                return "Reminder cannot be set in the past"
            }
        }

        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                category = category,
                priority = priority,
                dueDate = dueDate,
                dueTime = dueTime,
                reminderTime = reminderTimeMs,
                repeatOption = repeatOption,
                colorLabel = colorLabel
            )
            val insertId = repository.insertTask(task)
            
            // Register WorkManager if notification enabled
            if (notificationsEnabled.value && reminderTimeMs != null) {
                com.example.worker.ReminderWorker.scheduleReminder(
                    context = repository.allTasksFlow.let { null } ?: return@launch, // will be configured shortly
                    taskId = insertId,
                    triggerTimeMs = reminderTimeMs
                )
            }
        }
        return null
    }

    // Helper context for scheduling directly inside Launch
    fun insertTaskWithContext(
        context: android.content.Context,
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDate: Long?,
        dueTime: String?,
        reminderOffsetMin: Int?,
        colorLabel: String,
        repeatOption: String = "None"
    ): String? {
        if (title.trim().isEmpty()) {
            return "Task title cannot be empty"
        }

        var reminderTimeMs: Long? = null
        if (dueDate != null && reminderOffsetMin != null && reminderOffsetMin >= 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = dueDate
                if (dueTime != null) {
                    val timeParts = dueTime.split(":")
                    if (timeParts.size == 2) {
                        set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 12)
                        set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
                    }
                }
            }
            cal.add(Calendar.MINUTE, -reminderOffsetMin)
            reminderTimeMs = cal.timeInMillis

            if (reminderTimeMs < System.currentTimeMillis()) {
                return "Reminder cannot be set in the past"
            }
        }

        viewModelScope.launch {
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                category = category,
                priority = priority,
                dueDate = dueDate,
                dueTime = dueTime,
                reminderTime = reminderTimeMs,
                repeatOption = repeatOption,
                colorLabel = colorLabel
            )
            val id = repository.insertTask(task)
            if (reminderTimeMs != null && notificationsEnabled.value) {
                ReminderWorker.scheduleReminder(context.applicationContext, id, reminderTimeMs)
            }
        }
        return null
    }

    fun updateTaskWithContext(
        context: android.content.Context,
        task: TaskEntity,
        reminderOffsetMin: Int? = null
    ) {
        viewModelScope.launch {
            var finalReminderTimeMs: Long? = task.reminderTime
            
            if (reminderOffsetMin != null && task.dueDate != null) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = task.dueDate
                    if (task.dueTime != null) {
                        val timeParts = task.dueTime.split(":")
                        if (timeParts.size == 2) {
                            set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 12)
                            set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
                        }
                    }
                }
                cal.add(Calendar.MINUTE, -reminderOffsetMin)
                finalReminderTimeMs = cal.timeInMillis
            }

            val updatedTask = task.copy(
                reminderTime = finalReminderTimeMs,
                updatedDate = System.currentTimeMillis()
            )
            repository.updateTask(updatedTask)

            // Reschedule WorkManager
            ReminderWorker.cancelReminder(context.applicationContext, task.id)
            if (updatedTask.reminderTime != null && !updatedTask.isCompleted && !updatedTask.isArchived && notificationsEnabled.value) {
                ReminderWorker.scheduleReminder(
                    context.applicationContext,
                    updatedTask.id,
                    updatedTask.reminderTime
                )
            }
        }
    }

    fun duplicateTask(context: android.content.Context, task: TaskEntity) {
        viewModelScope.launch {
            val copied = task.copy(
                id = 0,
                title = "${task.title} (Copy)",
                createdDate = System.currentTimeMillis(),
                updatedDate = System.currentTimeMillis(),
                isCompleted = false
            )
            val newId = repository.insertTask(copied)
            if (copied.reminderTime != null && copied.reminderTime > System.currentTimeMillis() && notificationsEnabled.value) {
                ReminderWorker.scheduleReminder(context.applicationContext, newId, copied.reminderTime)
            }
        }
    }

    fun toggleTaskCompletion(context: android.content.Context, task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                updatedDate = System.currentTimeMillis()
            )
            repository.updateTask(updated)
            if (updated.isCompleted) {
                ReminderWorker.cancelReminder(context.applicationContext, task.id)
            } else if (updated.reminderTime != null && updated.reminderTime > System.currentTimeMillis() && notificationsEnabled.value) {
                ReminderWorker.scheduleReminder(context.applicationContext, task.id, updated.reminderTime)
            }
        }
    }

    fun toggleTaskPin(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isPinned = !task.isPinned, updatedDate = System.currentTimeMillis()))
        }
    }

    fun toggleTaskFavorite(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isFavorite = !task.isFavorite, updatedDate = System.currentTimeMillis()))
        }
    }

    fun toggleTaskArchive(context: android.content.Context, task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(isArchived = !task.isArchived, updatedDate = System.currentTimeMillis())
            repository.updateTask(updated)
            if (updated.isArchived) {
                ReminderWorker.cancelReminder(context.applicationContext, task.id)
            } else if (updated.reminderTime != null && updated.reminderTime > System.currentTimeMillis() && !updated.isCompleted && notificationsEnabled.value) {
                ReminderWorker.scheduleReminder(context.applicationContext, task.id, updated.reminderTime)
            }
        }
    }

    fun deleteTask(context: android.content.Context, task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            ReminderWorker.cancelReminder(context.applicationContext, task.id)
        }
    }

    fun insertScheduledTasks(
        context: android.content.Context,
        title: String,
        description: String,
        category: String,
        priority: String,
        colorLabel: String,
        dueTime: String?,
        reminderOffsetMin: Int?,
        dates: List<Long>,
        scheduleGroupId: String,
        scheduleConfigJson: String
    ) {
        viewModelScope.launch {
            dates.forEach { date ->
                var reminderTimeMs: Long? = null
                if (reminderOffsetMin != null && reminderOffsetMin >= 0) {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = date
                        if (dueTime != null) {
                            val timeParts = dueTime.split(":")
                            if (timeParts.size == 2) {
                                set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 12)
                                set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
                            }
                        }
                    }
                    cal.add(Calendar.MINUTE, -reminderOffsetMin)
                    reminderTimeMs = cal.timeInMillis
                }

                val task = TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    dueDate = date,
                    dueTime = dueTime,
                    reminderTime = reminderTimeMs,
                    repeatOption = "Custom",
                    colorLabel = colorLabel,
                    scheduleGroupId = scheduleGroupId,
                    scheduleConfig = scheduleConfigJson
                )
                val newId = repository.insertTask(task)
                if (reminderTimeMs != null && reminderTimeMs > System.currentTimeMillis() && notificationsEnabled.value) {
                    ReminderWorker.scheduleReminder(context.applicationContext, newId, reminderTimeMs)
                }
            }
        }
    }

    fun updateScheduledTasksGroup(
        context: android.content.Context,
        groupId: String,
        title: String,
        description: String,
        category: String,
        priority: String,
        colorLabel: String,
        dueTime: String?,
        reminderOffsetMin: Int?,
        dates: List<Long>,
        scheduleConfigJson: String
    ) {
        viewModelScope.launch {
            val existingTasks = repository.getTasksByGroupId(groupId)
            
            // Delete existing ones
            repository.deleteTasksByGroupId(groupId)
            // Cancel reminders
            existingTasks.forEach { 
                ReminderWorker.cancelReminder(context.applicationContext, it.id)
            }

            // Insert new scheduled tasks
            dates.forEach { date ->
                var reminderTimeMs: Long? = null
                if (reminderOffsetMin != null && reminderOffsetMin >= 0) {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = date
                        if (dueTime != null) {
                            val timeParts = dueTime.split(":")
                            if (timeParts.size == 2) {
                                set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 12)
                                set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
                            }
                        }
                    }
                    cal.add(Calendar.MINUTE, -reminderOffsetMin)
                    reminderTimeMs = cal.timeInMillis
                }

                val task = TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority,
                    dueDate = date,
                    dueTime = dueTime,
                    reminderTime = reminderTimeMs,
                    repeatOption = "Custom",
                    colorLabel = colorLabel,
                    scheduleGroupId = groupId,
                    scheduleConfig = scheduleConfigJson
                )
                val newId = repository.insertTask(task)
                if (reminderTimeMs != null && reminderTimeMs > System.currentTimeMillis() && notificationsEnabled.value) {
                    ReminderWorker.scheduleReminder(context.applicationContext, newId, reminderTimeMs)
                }
            }
        }
    }

    // MULTI-SELECT/BULK ACTIONS
    fun toggleTaskSelection(id: Long) {
        val current = selectedTaskIds.value
        selectedTaskIds.value = if (current.contains(id)) current - id else current + id
    }

    fun clearSelections() {
        selectedTaskIds.value = emptySet()
    }

    fun bulkComplete(context: android.content.Context, completed: Boolean) {
        val ids = selectedTaskIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.updateTasksCompletionStatus(ids, completed)
            if (completed) {
                ids.forEach { ReminderWorker.cancelReminder(context.applicationContext, it) }
            }
            clearSelections()
        }
    }

    fun bulkDelete(context: android.content.Context) {
        val ids = selectedTaskIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteMultipleTasks(ids)
            ids.forEach { ReminderWorker.cancelReminder(context.applicationContext, it) }
            clearSelections()
        }
    }

    // CATEGORY CRUD
    fun addCategory(name: String, iconName: String, colorHex: String) {
        if (name.trim().isEmpty()) return
        viewModelScope.launch {
            repository.insertCategory(
                CategoryEntity(
                    name = name.trim(),
                    iconName = iconName,
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategoryById(id)
        }
    }

    // PREFERENCES WRAPPERS
    fun setTheme(theme: String) {
        viewModelScope.launch {
            repository.preferences.saveTheme(theme)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean, context: android.content.Context) {
        viewModelScope.launch {
            repository.preferences.saveNotificationsEnabled(enabled)
            if (!enabled) {
                // Cancel all active reminders
                val tasks = allTasks.value
                for (task in tasks) {
                    ReminderWorker.cancelReminder(context, task.id)
                }
            } else {
                // Reschedule pending active tasks
                val tasks = allTasks.value
                for (task in tasks) {
                    if (!task.isCompleted && !task.isArchived && task.reminderTime != null && task.reminderTime > System.currentTimeMillis()) {
                        ReminderWorker.scheduleReminder(context, task.id, task.reminderTime)
                    }
                }
            }
        }
    }

    fun setFontSize(scale: Float) {
        viewModelScope.launch {
            repository.preferences.saveFontSize(scale)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            repository.preferences.saveLanguage(language)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
        }
    }

    // DB BACKUP triggers
    fun backupDb(): Boolean = repository.backupDatabase()
    fun restoreDb(): Boolean = repository.restoreDatabase()

    // CSV LOGIC
    fun getCsvString(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val tasks = allTasks.value
            val result = repository.exportToCsv(tasks)
            onResult(result)
        }
    }

    fun importCsv(csvContent: String, onCompleted: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.importFromCsv(csvContent)
            onCompleted(count)
        }
    }

    // AI-POWERED VOICE AND DIRECT TEXT STATEMENT PARSER WITH LOCAL ROBUST FALLBACKS
    fun parseVoiceCommandAi(
        context: android.content.Context,
        input: String,
        onFinished: (String?) -> Unit
    ) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            onFinished("Command was empty")
            return
        }

        viewModelScope.launch {
            try {
                val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                val todayStr = sdf.format(Date())
                val epochMs = System.currentTimeMillis()

                val parsed = com.example.data.SmartSuggestionHelper.parseNaturalLanguageTask(
                    inputText = trimmed,
                    currentLocalDateDesc = todayStr,
                    currentEpochMs = epochMs
                )

                val resultMsg = insertTaskWithContext(
                    context = context,
                    title = parsed.title,
                    description = if (parsed.description.isEmpty()) "Created via AI Smart Assistant" else parsed.description,
                    category = parsed.category,
                    priority = parsed.priority,
                    dueDate = parsed.dueDateMs,
                    dueTime = parsed.dueTime,
                    reminderOffsetMin = if (parsed.reminderTimeMs != null) 30 else null,
                    colorLabel = "#005FB0",
                    repeatOption = parsed.repeatOption
                )
                onFinished(resultMsg)
            } catch (e: Exception) {
                // Return immediate local parser fallback results
                val fallbackResult = parseVoiceCommand(context, trimmed)
                if (fallbackResult == null) {
                    onFinished(null) // resolved successfully with fallback!
                } else {
                    onFinished("AI Parse failed: ${e.localizedMessage}. Fallback execution: $fallbackResult")
                }
            }
        }
    }

    // VOICE COMMAND QUICK-PARSER
    fun parseVoiceCommand(context: android.content.Context, input: String): String? {
        val lowercase = input.lowercase().trim()
        if (lowercase.isEmpty()) return "Command was empty"

        // Template: "add project launch due tomorrow at 5pm priority high"
        // Try to find custom title
        // Simple extraction: trigger is "add <title>"
        if (lowercase.startsWith("add ")) {
            val content = lowercase.substring(4)
            val parts = content.split(" due ")
            val titlePart = parts[0].trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            
            var dueDate: Long? = null
            var dueTime: String? = null
            var priority = "Medium"

            if (parts.size > 1) {
                val subParts = parts[1].split(" priority ")
                val timeStr = subParts[0]
                
                // Parse "tomorrow" / "today"
                val cal = Calendar.getInstance()
                if (timeStr.contains("tomorrow")) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    dueDate = cal.timeInMillis
                } else if (timeStr.contains("today")) {
                    dueDate = cal.timeInMillis
                }

                if (subParts.size > 1) {
                    val p = subParts[1].trim()
                    if (p.contains("low")) priority = "Low"
                    if (p.contains("high")) priority = "High"
                    if (p.contains("urgent")) priority = "Urgent"
                }
            }

            return insertTaskWithContext(
                context = context,
                title = titlePart,
                description = "Created via Voice Command",
                category = "Personal",
                priority = priority,
                dueDate = dueDate,
                dueTime = dueTime,
                reminderOffsetMin = if (dueDate != null) 30 else null,
                colorLabel = "#4CAF50"
            )
        }
        return "Unrecognized command template"
    }

    // PRODUCTIVITY & STATS ALGORITHMS
    // Calculate custom stats dynamically from complete local data!
    fun getTaskStats(): TaskStats {
        val tasks = allTasks.value
        val activeTasks = tasks.filter { !it.isArchived }

        val total = activeTasks.size
        val completed = activeTasks.count { it.isCompleted }
        val pending = total - completed
        val archived = tasks.count { it.isArchived }
        
        // Overdue pending tasks
        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val overdue = activeTasks.count { 
            !it.isCompleted && it.dueDate != null && it.dueDate < todayCalendar.timeInMillis 
        }

        val completionRate = if (total > 0) (completed.toFloat() / total * 100).toInt() else 0

        // Daily task distribution
        val completionsByDay = mutableMapOf<String, Int>()
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()
        
        // Prepopulate last 7 days with 0
        for (i in 0..6) {
            val d = cal.time
            completionsByDay[sdf.format(d)] = 0
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Fill completions
        for (t in tasks) {
            if (t.isCompleted) {
                val taskDateStr = sdf.format(Date(t.updatedDate))
                if (completionsByDay.containsKey(taskDateStr)) {
                    completionsByDay[taskDateStr] = (completionsByDay[taskDateStr] ?: 0) + 1
                }
            }
        }

        // Daily streak calculation from actual data
        var currentStreak = 0
        val compDates = tasks.filter { it.isCompleted }
            .map { 
                val c = Calendar.getInstance().apply { timeInMillis = it.updatedDate }
                "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}-${c.get(Calendar.DAY_OF_MONTH)}"
            }
            .toSet()

        val checkCal = Calendar.getInstance()
        while (true) {
            val dateStr = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.MONTH)}-${checkCal.get(Calendar.DAY_OF_MONTH)}"
            if (compDates.contains(dateStr)) {
                currentStreak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return TaskStats(
            total = total,
            completed = completed,
            pending = pending,
            archived = archived,
            overdue = overdue,
            completionRate = completionRate,
            currentStreak = currentStreak,
            chartWeeklyCompletions = completionsByDay.entries.reversed().map { ChartData(it.key, it.value) }
        )
    }
}

data class TaskStats(
    val total: Int,
    val completed: Int,
    val pending: Int,
    val archived: Int,
    val overdue: Int,
    val completionRate: Int,
    val currentStreak: Int,
    val chartWeeklyCompletions: List<ChartData>
)

data class ChartData(
    val label: String,
    val value: Int
)

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
