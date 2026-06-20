package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TaskEntity
import com.example.ui.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)

            // Header for selected day tasks
            val selectedDateStr = remember(selectedDate) {
                val sdf = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
                sdf.format(selectedDate.time)
            }
            Text(
                "Agenda for $selectedDateStr",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(selectedTasks) { task ->
                        val isOverdue = remember(task) {
                            val today = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            task.dueDate != null && task.dueDate < today && !task.isCompleted
                        }

                        Card(
                            onClick = { onNavigateToDetail(task.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOverdue) Color(0xFFFFDAD6) else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                            modifier = Modifier.fillMaxWidth()
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
                                            Text(
                                                text = task.dueTime!!,
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
                        Box(modifier = Modifier.weight(1.5f)) // Blank slot
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
