package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskEntity
import com.example.ui.TaskViewModel
import com.example.util.TimeScheduleUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val tasks by viewModel.allTasks.collectAsState()

    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    val monthName = remember(calendarMonth) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        sdf.format(calendarMonth.time)
    }

    // List of tasks scheduled for selectedDate
    val selectedTasks = remember(tasks, selectedDate) {
        tasks.filter { task ->
            if (task.dueDate == null) return@filter false
            val taskCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
            taskCal.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    taskCal.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR) &&
                    !task.isArchived
        }
    }

    // 1. Toggle for Agenda View Type: "Standard List" vs "Clock Timeline"
    var viewType by remember { mutableStateOf("Timeline") } // "List" vs "Timeline"
    
    // 2. Toggle for Time Format: 12-Hour vs 24-Hour
    var use12HourFormat by remember { mutableStateOf(true) }

    // 3. For Focus Theater Mode Overlay
    var focusedTaskByTheater by remember { mutableStateOf<TaskEntity?>(null) }

    // 4. Current System minutes to compute dynamic progress/highlights
    var currentSystemMinutes by remember {
        mutableStateOf(
            Calendar.getInstance().apply {}.let { cal ->
                cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            }
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            currentSystemMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            kotlinx.coroutines.delay(10000) // tick every 10 seconds
        }
    }

    // Solve Duplicating Entire Schedule template to another date
    var showDuplicatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Task Calendar", 
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
                    // Quick Action: Duplicate scheduled items to another date!
                    if (selectedTasks.isNotEmpty()) {
                        IconButton(onClick = { showDuplicatePicker = true }) {
                            Icon(Icons.Default.ContentCopy, "Duplicate Day Schedule", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    // Format Switcher Action
                    IconButton(onClick = { use12HourFormat = !use12HourFormat }) {
                        Text(
                            text = if (use12HourFormat) "12H" else "24H",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month switcher header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val cloned = calendarMonth.clone() as Calendar
                    cloned.add(Calendar.MONTH, -1)
                    calendarMonth = cloned
                }) {
                    Icon(Icons.Default.ChevronLeft, "Previous Month")
                }

                Text(
                    text = monthName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = {
                    val cloned = calendarMonth.clone() as Calendar
                    cloned.add(Calendar.MONTH, 1)
                    calendarMonth = cloned
                }) {
                    Icon(Icons.Default.ChevronRight, "Next Month")
                }
            }

            // Calendar Month Grid View
            CalendarGrid(
                calendarMonth = calendarMonth,
                selectedDate = selectedDate,
                allTasks = tasks,
                onDayClick = { day ->
                    selectedDate = day
                }
            )

            // Selector Tab for Agenda list vs Timeline view with beautiful design
            TabRow(
                selectedTabIndex = if (viewType == "Timeline") 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Tab(
                    selected = viewType == "Timeline",
                    onClick = { viewType = "Timeline" },
                    text = { Text("Clock Timeline", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = viewType == "List",
                    onClick = { viewType = "List" },
                    text = { Text("List Agenda", fontWeight = FontWeight.SemiBold) }
                )
            }

            // Header for selected day tasks with dynamic statistics
            val selectedDateStr = remember(selectedDate) {
                val sdf = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
                sdf.format(selectedDate.time)
            }

            // Daily summaries analytics calculation
            val (totalScheduledMins, completedMins, freeMins) = remember(selectedTasks) {
                val wakingMins = 17 * 60 // 6:00 AM to 11:00 PM is 1020 minutes
                var total = 0
                var completed = 0
                selectedTasks.forEach { t ->
                    val s = TimeScheduleUtils.parseToMinutes(t.dueTime) ?: 540
                    val e = TimeScheduleUtils.parseToMinutes(t.endTime) ?: (s + t.estimatedDuration)
                    val dur = (e - s).coerceAtLeast(15)
                    total += dur
                    if (t.isCompleted) completed += dur
                }
                val free = (wakingMins - total).coerceAtLeast(0)
                Triple(total, completed, free)
            }

            if (viewType == "Timeline") {
                // Show a mini stats analytics board
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Scheduled", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.getDefault(), "%.1fh", totalScheduledMins / 60f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Completed", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.getDefault(), "%.1fh", completedMins / 60f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Free Time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.getDefault(), "%.1fh", freeMins / 60f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            Text(
                "Agenda for $selectedDateStr",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Dynamic task schedule list
            if (selectedTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No tasks scheduled for this day.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (viewType == "Timeline") {
                    // Visual clock-based Scheduler View
                    TimelineView(
                        tasksOnDay = selectedTasks,
                        currentMinutes = currentSystemMinutes,
                        use12H = use12HourFormat,
                        onNavigateToDetail = onNavigateToDetail,
                        onFocusTheater = { focusedTaskByTheater = it },
                        onQuickShift = { task, minutesShift ->
                            val s = TimeScheduleUtils.parseToMinutes(task.dueTime) ?: 540
                            val e = TimeScheduleUtils.parseToMinutes(task.endTime) ?: (s + task.estimatedDuration)
                            val newS = (s + minutesShift + 1440) % 1440
                            val newE = (e + minutesShift + 1440) % 1440
                            
                            val updatedTask = task.copy(
                                dueTime = TimeScheduleUtils.formatTo24H(newS),
                                endTime = TimeScheduleUtils.formatTo24H(newE),
                                updatedDate = System.currentTimeMillis()
                            )
                            viewModel.updateTaskWithContext(context, updatedTask)
                        },
                        onFixConflict = { targetTask ->
                            // Magical conflict solver finder
                            val freeSlotStart = TimeScheduleUtils.findAvailableSlot(selectedTasks, targetTask.estimatedDuration)
                            val updatedTask = targetTask.copy(
                                dueTime = TimeScheduleUtils.formatTo24H(freeSlotStart),
                                endTime = TimeScheduleUtils.formatTo24H(freeSlotStart + targetTask.estimatedDuration),
                                updatedDate = System.currentTimeMillis()
                            )
                            viewModel.updateTaskWithContext(context, updatedTask)
                            android.widget.Toast.makeText(context, "Task moved to conflict-free slot!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    // Standard List view with countdowns & stats indicators
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(selectedTasks, key = { it.id }) { task ->
                            val isOverdue = remember(task, currentSystemMinutes) {
                                if (task.isCompleted) return@remember false
                                val taskStartMinutes = TimeScheduleUtils.parseToMinutes(task.dueTime) ?: 0
                                taskStartMinutes < currentSystemMinutes
                            }

                            Card(
                                onClick = { onNavigateToDetail(task.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isOverdue) Color(0xFFFFDAD6) else MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            task.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isOverdue) Color(0xFF410002) else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (task.description.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                task.description,
                                                fontSize = 12.sp,
                                                color = if (isOverdue) Color(0xFF410002).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 1
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Category, priority and time details
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = task.category,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            val (priorityBg, priorityFg) = when (task.priority.lowercase()) {
                                                "urgent", "high" -> Pair(Color(0xFFFFDAD6), Color(0xFF410002))
                                                "medium" -> Pair(Color(0xFFE2E2E6), Color(0xFF44474E))
                                                else -> Pair(Color(0xFFF1F3F9), Color(0xFF74777F))
                                            }

                                            Surface(
                                                color = priorityBg,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(18.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(priorityFg)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = task.priority.uppercase(),
                                                        fontSize = 8.sp,
                                                        color = priorityFg,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            if (task.dueTime != null) {
                                                Icon(
                                                    imageVector = Icons.Default.Timer,
                                                    contentDescription = "Time",
                                                    modifier = Modifier.size(11.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                val startMin = TimeScheduleUtils.parseToMinutes(task.dueTime) ?: 0
                                                val displayTimeStr = if (use12HourFormat) TimeScheduleUtils.formatTo12H(startMin) else task.dueTime!!
                                                Text(
                                                    text = displayTimeStr,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (isOverdue) {
                                        Surface(
                                            color = Color(0xFFF44336),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "OVERDUE",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Solve schedule template duplication (Copy today's schedule to custom date)
    if (showDuplicatePicker) {
        var duplicateTargetDateMs by remember { mutableStateOf<Long?>(null) }
        AlertDialog(
            onDismissRequest = { showDuplicatePicker = false },
            title = { Text("Duplicate Day Schedule") },
            text = {
                Column {
                    Text("Select a future target date to clone these ${selectedTasks.size} tasks into:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    val dateLabel = duplicateTargetDateMs?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "No Date Selected"
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val target = Calendar.getInstance()
                                    target.set(year, month, dayOfMonth)
                                    duplicateTargetDateMs = target.timeInMillis
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
                        Text(dateLabel)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tDate = duplicateTargetDateMs
                        if (tDate != null) {
                            selectedTasks.forEach { task ->
                                val copied = task.copy(
                                    id = 0,
                                    dueDate = tDate,
                                    isCompleted = false,
                                    createdDate = System.currentTimeMillis(),
                                    updatedDate = System.currentTimeMillis()
                                )
                                viewModel.insertTaskWithContext(
                                    context = context,
                                    title = copied.title,
                                    description = copied.description,
                                    category = copied.category,
                                    priority = copied.priority,
                                    dueDate = copied.dueDate,
                                    dueTime = copied.dueTime,
                                    reminderOffsetMin = 15,
                                    colorLabel = copied.colorLabel,
                                    endTime = copied.endTime,
                                    estimatedDuration = copied.estimatedDuration
                                )
                            }
                            android.widget.Toast.makeText(context, "Successfully duplicated schedule!", android.widget.Toast.LENGTH_LONG).show()
                            showDuplicatePicker = false
                        }
                    },
                    enabled = duplicateTargetDateMs != null
                ) {
                    Text("Duplicate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicatePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5. Theater Focus Mode Screen overlay
    if (focusedTaskByTheater != null) {
        val fTask = focusedTaskByTheater!!
        val startMins = TimeScheduleUtils.parseToMinutes(fTask.dueTime) ?: 540
        val endMins = TimeScheduleUtils.parseToMinutes(fTask.endTime) ?: (startMins + fTask.estimatedDuration)
        val durationMins = endMins - startMins
        val remaining = (endMins - currentSystemMinutes).coerceAtLeast(0)

        // Focus Dialog
        AlertDialog(
            onDismissRequest = { focusedTaskByTheater = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Active Task Focus Mode", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text(
                        text = fTask.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (fTask.description.isEmpty()) "No description provided." else fTask.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Circular Countdown Graphic
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                        val progress = if (durationMins > 0) remaining.toFloat() / durationMins else 1f
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(130.dp),
                            strokeWidth = 8.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$remaining",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Mins left",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    LinearProgressIndicator(
                        progress = { if (durationMins > 0) (durationMins - remaining).toFloat() / durationMins else 1f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFFE0E0E0)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Start: ${if (use12HourFormat) TimeScheduleUtils.formatTo12H(startMins) else fTask.dueTime}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text("End: ${if (use12HourFormat) TimeScheduleUtils.formatTo12H(endMins) else fTask.endTime}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleTaskCompletion(context, fTask)
                        focusedTaskByTheater = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Complete Task")
                }
            },
            dismissButton = {
                TextButton(onClick = { focusedTaskByTheater = null }) {
                    Text("Exit Focus")
                }
            }
        )
    }
}

@Composable
fun TimelineView(
    tasksOnDay: List<TaskEntity>,
    currentMinutes: Int,
    use12H: Boolean,
    onNavigateToDetail: (Long) -> Unit,
    onFocusTheater: (TaskEntity) -> Unit,
    onQuickShift: (TaskEntity, Int) -> Unit,
    onFixConflict: (TaskEntity) -> Unit
) {
    // Detect schedule conflict overlaps
    val conflicts = remember(tasksOnDay) {
        TimeScheduleUtils.findConflicts(tasksOnDay)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Red alert box for overlapping scheduled task slots
        if (conflicts.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Overlap Warnings", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${conflicts.size} schedule conflicts detected!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Some tasks occupy overlapping times. Tap Fix on any card to automatically shift to a free slot.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Render hours of the day (e.g. 6:00 AM to 11:00 PM)
        for (hour in 6..23) {
            val hourStartMins = hour * 60
            val hourEndMins = (hour + 1) * 60

            val tasksInHour = remember(tasksOnDay, hour) {
                tasksOnDay.filter { task ->
                    val s = TimeScheduleUtils.parseToMinutes(task.dueTime) ?: 0
                    val e = TimeScheduleUtils.parseToMinutes(task.endTime) ?: (s + task.estimatedDuration)
                    // Does this task include any part of this hour?
                    s < hourEndMins && e > hourStartMins
                }
            }

            // Hour Line Block
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Hour Label
                val timeLabel = if (use12H) TimeScheduleUtils.formatTo12H(hourStartMins) else String.format(Locale.getDefault(), "%02d:00", hour)
                Text(
                    text = timeLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(68.dp).padding(vertical = 4.dp),
                    textAlign = TextAlign.End
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Vertical dash or timeline line
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Current system time pointer line
                    if (currentMinutes >= hourStartMins && currentMinutes < hourEndMins) {
                        Surface(
                            color = Color(0xFFE53935),
                            shape = CircleShape,
                            modifier = Modifier.fillMaxWidth().height(2.dp).padding(end = 6.dp)
                        ) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFE53935)))
                            }
                        }
                    } else {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (tasksInHour.isEmpty()) {
                        Text(
                            "Free Slot",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        tasksInHour.forEach { task ->
                            val s = TimeScheduleUtils.parseToMinutes(task.dueTime) ?: 540
                            val e = TimeScheduleUtils.parseToMinutes(task.endTime) ?: (s + task.estimatedDuration)
                            val isActive = currentMinutes in s until e
                            val hasOverlaps = conflicts.any { it.first.id == task.id || it.second.id == task.id }

                            // Highlight border for active task
                            val borderSpec = if (isActive) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else if (hasOverlaps) {
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
                            }

                            Card(
                                onClick = { onNavigateToDetail(task.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = borderSpec,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                    else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Color Label Circle
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(task.colorLabel.ifEmpty { "#2196F3" })))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = task.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Status badge
                                        if (isActive) {
                                            Surface(
                                                color = Color(0xFFFF9800),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "ACTIVE",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        } else if (task.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Done",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Time labels display
                                    val startFormatted = if (use12H) TimeScheduleUtils.formatTo12H(s) else task.dueTime
                                    val endFormatted = if (use12H) TimeScheduleUtils.formatTo12H(e) else task.endTime
                                    Text(
                                        text = "$startFormatted - $endFormatted (${task.estimatedDuration}m)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (isActive && !task.isCompleted) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val remainingMins = (e - currentMinutes).coerceAtLeast(0)
                                        Text(
                                            text = "⏳ $remainingMins mins left",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        // Progress bar
                                        val dur = (e - s).coerceAtLeast(1)
                                        val progress = (currentMinutes - s).toFloat() / dur
                                        LinearProgressIndicator(
                                            progress = { progress.coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else if (s > currentMinutes && !task.isCompleted) {
                                        val diff = s - currentMinutes
                                        Text(
                                            text = "🕒 Starts in $diff mins",
                                            fontSize = 10.sp,
                                            color = Color(0xFF2E7D32)
                                        )
                                    } else if (!task.isCompleted && currentMinutes >= e) {
                                        Text(
                                            text = "⚠️ Overdue",
                                            fontSize = 10.sp,
                                            color = Color(0xFFC62828),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Easy timing adjustment & dynamic actions block
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Focus action button
                                        if (isActive && !task.isCompleted) {
                                            OutlinedButton(
                                                onClick = { onFocusTheater(task) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Focus", fontSize = 9.sp)
                                            }
                                        }

                                        // Auto-Solver Conflict action
                                        if (hasOverlaps) {
                                            Button(
                                                onClick = { onFixConflict(task) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Fix Slot", fontSize = 9.sp)
                                            }
                                        }

                                        // Micro Timing Adjustment Actions
                                        IconButton(
                                            onClick = { onQuickShift(task, -15) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ChevronLeft, "Delay Day", modifier = Modifier.size(14.dp))
                                        }
                                        Text("-15m", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                        
                                        IconButton(
                                            onClick = { onQuickShift(task, 15) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ChevronRight, "Advance Day", modifier = Modifier.size(14.dp))
                                        }
                                        Text("+15m", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    calendarMonth: Calendar,
    selectedDate: Calendar,
    allTasks: List<TaskEntity>,
    onDayClick: (Calendar) -> Unit
) {
    val daysInMonth = calendarMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    // Day of the week of first day of the month (1 = Sunday, 2 = Monday...)
    val firstDayCal = calendarMonth.clone() as Calendar
    firstDayCal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)

    val prefixes = firstDayOfWeek - 1 // Blank spaces before 1st of the Month

    // Weekdays labels
    val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Render headings
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Grid days logic
        var currentDay = 1
        val today = Calendar.getInstance()

        for (row in 0..5) {
            if (currentDay > daysInMonth) break
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                for (col in 1..7) {
                    val index = row * 7 + col
                    val isBlank = index <= prefixes || currentDay > daysInMonth

                    if (isBlank) {
                        Box(modifier = Modifier.weight(1f)) // Blank slot
                    } else {
                        val dayValue = currentDay
                        val itemCal = calendarMonth.clone() as Calendar
                        itemCal.set(Calendar.DAY_OF_MONTH, dayValue)

                        val isSelected = selectedDate.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                                selectedDate.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)

                        val isToday = today.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                                today.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)

                        // Check tasks on this day for colors or indicators
                        val dayTasks = allTasks.filter { task ->
                            if (task.dueDate == null) return@filter false
                            val tCal = Calendar.getInstance().apply { timeInMillis = task.dueDate }
                            tCal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                                    tCal.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR) &&
                                    !task.isArchived
                        }
                        val hasOverdue = dayTasks.any { !it.isCompleted && it.dueDate!! < today.timeInMillis }

                        val cellBg = when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            isToday -> MaterialTheme.colorScheme.secondaryContainer
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBg)
                                .clickable { onDayClick(itemCal) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayValue.toString(),
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                
                                // Little dot indicators below date
                                if (dayTasks.isNotEmpty()) {
                                    val dotColor = if (hasOverdue) Color.Red else MaterialTheme.colorScheme.primary
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                }
                            }
                        }
                        currentDay++
                    }
                }
            }
        }
    }
}
