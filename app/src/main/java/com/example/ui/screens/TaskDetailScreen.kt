package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.data.TaskEntity
import com.example.data.SmartSuggestionHelper
import com.example.ui.TaskViewModel
import com.example.util.TimeScheduleUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long,
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val tasks by viewModel.allTasks.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val task = remember(tasks, taskId) { tasks.find { it.id == taskId } }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Task Details", 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    if (task != null) {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, task.title)
                                putExtra(Intent.EXTRA_TEXT, "${task.title}\n\n${task.description}\nCategory: ${task.category}\nPriority: ${task.priority}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Task via"))
                        }) {
                            Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { viewModel.duplicateTask(context, task) }) {
                            Icon(Icons.Default.ContentCopy, "Duplicate Task", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFC62828))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (task == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Task not found or has been deleted.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Task Card details in a high-contrast elegant white card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = task.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { viewModel.toggleTaskCompletion(context, task) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Description",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            
                            var isOptimizing by remember { mutableStateOf(false) }
                            val optScope = rememberCoroutineScope()
                            
                            if (isOptimizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            isOptimizing = true
                                            optScope.launch {
                                                try {
                                                    val optDesc = SmartSuggestionHelper.optimizeTaskDescription(task)
                                                    viewModel.updateTaskWithContext(context, task.copy(description = optDesc))
                                                    android.widget.Toast.makeText(context, "Description optimized with AI!", android.widget.Toast.LENGTH_SHORT).show()
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "AI Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                                } finally {
                                                    isOptimizing = false
                                                }
                                            }
                                        }
                                        .padding(4.dp)
                                        .testTag("ai_optimize_desc_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Optimize",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Optimize with AI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (task.description.isEmpty()) "No description provided." else task.description,
                            fontSize = 14.sp,
                            color = if (task.description.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Attributes List inside its own nice white card for clear container segregation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AttributeRow(icon = Icons.Default.Label, label = "Category", value = task.category)
                        
                        val pColor = when(task.priority.lowercase()) {
                            "urgent" -> Color(0xFFC62828)
                            "high" -> Color(0xFFD66000)
                            "medium" -> Color(0xFF005FB0)
                            else -> Color(0xFF2E7D32)
                        }
                        AttributeRow(
                            icon = Icons.Default.PriorityHigh, 
                            label = "Priority", 
                            value = task.priority,
                            textColor = pColor
                        )

                        val df = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                        val dateValue = task.dueDate?.let { df.format(Date(it)) } ?: "No due date"
                        AttributeRow(icon = Icons.Default.CalendarToday, label = "Due Date", value = dateValue)

                        if (task.dueTime != null) {
                            val formattedStart = TimeScheduleUtils.parseToMinutes(task.dueTime)?.let { TimeScheduleUtils.formatTo12H(it) } ?: task.dueTime
                            AttributeRow(icon = Icons.Default.AccessTime, label = "Start Time", value = formattedStart)
                        }

                        if (task.endTime != null) {
                            val formattedEnd = TimeScheduleUtils.parseToMinutes(task.endTime)?.let { TimeScheduleUtils.formatTo12H(it) } ?: task.endTime
                            AttributeRow(icon = Icons.Default.Timer, label = "End Time", value = formattedEnd)
                        }

                        AttributeRow(icon = Icons.Default.HourglassEmpty, label = "Duration", value = "${task.estimatedDuration} minutes")

                        val reminderText = if (task.reminderTime != null) {
                            val tf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                            "Set for ${tf.format(Date(task.reminderTime))}"
                        } else {
                            "No reminder set"
                        }
                        AttributeRow(icon = Icons.Default.NotificationsActive, label = "Reminder", value = reminderText)

                        AttributeRow(icon = Icons.Default.Repeat, label = "Repeat Option", value = task.repeatOption)

                        val statusStr = if (task.isCompleted) "Completed" else "Pending"
                        AttributeRow(icon = Icons.Default.CheckCircle, label = "Status", value = statusStr, textColor = if (task.isCompleted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface)

                        val pinValue = if (task.isPinned) "Pinned to top" else "Standard list"
                        AttributeRow(icon = Icons.Default.PushPin, label = "Pin State", value = pinValue)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons at bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, "Edit")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Task")
                    }

                    OutlinedButton(
                        onClick = { viewModel.toggleTaskArchive(context, task) },
                        modifier = Modifier.weight(1f)
                    ) {
                        val label = if (task.isArchived) "Restore Task" else "Archive Task"
                        val iconImg = if (task.isArchived) Icons.Default.Unarchive else Icons.Default.Archive
                        Icon(iconImg, label)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label)
                    }
                }
            }
        }
    }

    // Edit task dialog
    if (showEditDialog && task != null) {
        var editTitle by remember { mutableStateOf(task.title) }
        var editDesc by remember { mutableStateOf(task.description) }
        var editCategory by remember { mutableStateOf(task.category) }
        var editPriority by remember { mutableStateOf(task.priority) }
        
        var editDueTime by remember { mutableStateOf(task.dueTime ?: "09:00") }
        var editEndTime by remember { mutableStateOf(task.endTime ?: "09:30") }
        var editDuration by remember { mutableStateOf(task.estimatedDuration) }
        var editDueDate by remember { mutableStateOf(task.dueDate ?: System.currentTimeMillis()) }

        val initialSettings = remember(task) { SchedulerSettings.fromJson(task.scheduleConfig) }
        var schedulerSettings by remember { mutableStateOf(initialSettings) }
        var scheduledDatesList by remember { 
            mutableStateOf(
                if (task.scheduleConfig != null) {
                    calculateScheduledDates(
                        mode = initialSettings.mode,
                        startDate = initialSettings.startDate ?: (task.dueDate ?: System.currentTimeMillis()),
                        endDate = initialSettings.endDate ?: (task.dueDate ?: System.currentTimeMillis()),
                        intervalDays = initialSettings.intervalDays,
                        selectedDates = initialSettings.selectedDates,
                        recurrentPreset = initialSettings.recurrentPreset,
                        selectedWeekdays = initialSettings.selectedWeekdays,
                        monthlyPattern = initialSettings.monthlyPattern,
                        endCondition = initialSettings.endCondition,
                        endConditionDate = initialSettings.endConditionDate,
                        endConditionOccurrences = initialSettings.endConditionOccurrences
                    )
                } else emptyList()
            )
        }
        var showSchedulerDialog by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Task Attributes") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select Date & Duration", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Date picker button
                    val dateFormatted = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(editDueDate))
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = editDueDate }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val selected = Calendar.getInstance()
                                    selected.set(year, month, dayOfMonth)
                                    editDueDate = selected.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Date: $dateFormatted")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Time Picker Button
                        OutlinedButton(
                            onClick = {
                                val parts = editDueTime.split(":")
                                val h = if (parts.size >= 2) parts[0].toIntOrNull() ?: 9 else 9
                                val m = if (parts.size >= 2) parts[1].toIntOrNull() ?: 0 else 0
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        editDueTime = String.format("%02d:%02d", hourOfDay, minute)
                                        // Automatically set endTime based on estimatedDuration
                                        val endMinutes = (hourOfDay * 60 + minute) + editDuration
                                        editEndTime = String.format("%02d:%02d", (endMinutes / 60) % 24, endMinutes % 60)
                                    },
                                    h,
                                    m,
                                    false
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            val formattedTime = TimeScheduleUtils.parseToMinutes(editDueTime)?.let { TimeScheduleUtils.formatTo12H(it) } ?: editDueTime
                            Text(text = "Start: $formattedTime", fontSize = 11.sp)
                        }

                        // End Time Picker Button
                        OutlinedButton(
                            onClick = {
                                val parts = editEndTime.split(":")
                                val h = if (parts.size >= 2) parts[0].toIntOrNull() ?: 9 else 9
                                val m = if (parts.size >= 2) parts[1].toIntOrNull() ?: 30 else 30
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        editEndTime = String.format("%02d:%02d", hourOfDay, minute)
                                        // Re-calculate duration if start time is set
                                        if (editDueTime.isNotEmpty()) {
                                            val startMin = TimeScheduleUtils.parseToMinutes(editDueTime) ?: 0
                                            val endMin = hourOfDay * 60 + minute
                                            val diff = (endMin - startMin + 1440) % 1440
                                            editDuration = if (diff > 0) diff else 30
                                        }
                                    },
                                    h,
                                    m,
                                    false
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            val formattedTime = TimeScheduleUtils.parseToMinutes(editEndTime)?.let { TimeScheduleUtils.formatTo12H(it) } ?: editEndTime
                            Text(text = "End: $formattedTime", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Duration Preset Chips
                    Text("Estimated Duration", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(15, 30, 45, 60, 120).forEach { mins ->
                            val label = if (mins >= 60) "${mins / 60}h" else "${mins}m"
                            val isSel = editDuration == mins
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    editDuration = mins
                                    if (editDueTime.isNotEmpty()) {
                                        val startMin = TimeScheduleUtils.parseToMinutes(editDueTime) ?: 540
                                        val endMin = startMin + mins
                                        editEndTime = String.format("%02d:%02d", (endMin / 60) % 24, endMin % 60)
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    // Priority Selector
                    Column {
                        Text("Priority", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Low", "Medium", "High", "Urgent").forEach { p ->
                                FilterChip(
                                    selected = editPriority == p,
                                    onClick = { editPriority = p },
                                    label = { Text(p) }
                                )
                            }
                        }
                    }

                    // Category Selector
                    Column {
                        Text("Category", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            categories.forEach { c ->
                                FilterChip(
                                    selected = editCategory == c.name,
                                    onClick = { editCategory = c.name },
                                    label = { Text(c.name) }
                                )
                            }
                        }
                    }

                    // Schedule editing segment
                    Column {
                        Text("Task Schedule", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        if (task.scheduleGroupId != null || scheduledDatesList.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSchedulerDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Edit Schedule",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Active Recurring Schedule",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "${if(scheduledDatesList.isNotEmpty()) scheduledDatesList.size else "Multiple"} occurrences scheduled. Tap to edit recurrence.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showSchedulerDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, "Add Schedule")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Convert to Recurring Schedule")
                            }
                        }
                    }

                    // Interactive Calendar Scheduler Modal
                    if (showSchedulerDialog) {
                        TaskSchedulerDialog(
                            taskTitle = editTitle,
                            initialSettings = schedulerSettings,
                            onDismiss = { showSchedulerDialog = false },
                            onSaveSchedule = { settings, dates ->
                                schedulerSettings = settings
                                scheduledDatesList = dates
                                showSchedulerDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isRecurrenceChanged = schedulerSettings != initialSettings
                        val isDateChanged = editDueDate != (task.dueDate ?: 0L)
                        
                        if (task.scheduleGroupId != null) {
                            if (isRecurrenceChanged) {
                                // 1. Recurrence pattern changed: regenerate the group.
                                // Since this deletes old tasks and recreates new ones, we navigate back.
                                viewModel.updateScheduledTasksGroup(
                                    context = context,
                                    groupId = task.scheduleGroupId,
                                    title = editTitle,
                                    description = editDesc,
                                    category = editCategory,
                                    priority = editPriority,
                                    colorLabel = task.colorLabel.ifEmpty { "#2196F3" },
                                    dueTime = editDueTime,
                                    endTime = editEndTime,
                                    estimatedDuration = editDuration,
                                    reminderOffsetMin = 15,
                                    dates = scheduledDatesList,
                                    scheduleConfigJson = schedulerSettings.toJson()
                                )
                                onNavigateBack()
                            } else if (isDateChanged) {
                                // 2. Date changed: move only this specific occurrence to a different day.
                                val updated = task.copy(
                                    title = editTitle,
                                    description = editDesc,
                                    category = editCategory,
                                    priority = editPriority,
                                    dueTime = editDueTime,
                                    endTime = editEndTime,
                                    estimatedDuration = editDuration,
                                    dueDate = editDueDate,
                                    updatedDate = System.currentTimeMillis()
                                )
                                viewModel.updateTaskWithContext(context, updated)
                            } else {
                                // 3. Common properties changed (Title, Description, Category, Priority, Times, Duration): 
                                // Update all tasks in the group, preserving their original IDs and scheduled dates.
                                viewModel.updateTaskGroupAttributes(
                                    context = context,
                                    groupId = task.scheduleGroupId,
                                    title = editTitle,
                                    description = editDesc,
                                    category = editCategory,
                                    priority = editPriority,
                                    colorLabel = task.colorLabel.ifEmpty { "#2196F3" },
                                    dueTime = editDueTime,
                                    endTime = editEndTime,
                                    estimatedDuration = editDuration
                                )
                            }
                        } else {
                            // 4. Update as single normal task
                            if (scheduledDatesList.isNotEmpty()) {
                                // User converted single task to a recurring task series!
                                val newGroupId = "group_${System.currentTimeMillis()}"
                                viewModel.updateScheduledTasksGroup(
                                    context = context,
                                    groupId = newGroupId,
                                    title = editTitle,
                                    description = editDesc,
                                    category = editCategory,
                                    priority = editPriority,
                                    colorLabel = task.colorLabel.ifEmpty { "#2196F3" },
                                    dueTime = editDueTime,
                                    endTime = editEndTime,
                                    estimatedDuration = editDuration,
                                    reminderOffsetMin = 15,
                                    dates = scheduledDatesList,
                                    scheduleConfigJson = schedulerSettings.toJson()
                                )
                                // Delete the original single task as it is now part of the recurring series
                                viewModel.deleteTask(context, task)
                                onNavigateBack()
                            } else {
                                val updated = task.copy(
                                    title = editTitle,
                                    description = editDesc,
                                    category = editCategory,
                                    priority = editPriority,
                                    dueTime = editDueTime,
                                    endTime = editEndTime,
                                    estimatedDuration = editDuration,
                                    dueDate = editDueDate,
                                    updatedDate = System.currentTimeMillis()
                                )
                                viewModel.updateTaskWithContext(context, updated)
                            }
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Save Updates")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete task confirm dialog
    if (showDeleteConfirm && task != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Task?") },
            text = { Text("Are you sure you want to permanently delete task '${task.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(context, task)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AttributeRow(
    icon: ImageVector,
    label: String,
    value: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 15.sp, color = textColor, fontWeight = FontWeight.Medium)
        }
    }
}
