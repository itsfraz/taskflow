package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

enum class SchedulingMode {
    RANGE,
    MULTI,
    PATTERN
}

// Data holder for scheduler settings
data class SchedulerSettings(
    val mode: SchedulingMode = SchedulingMode.RANGE,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val intervalDays: Int = 1,
    val selectedDates: Set<Long> = emptySet(),
    val recurrentPreset: String = "None", // Daily, Weekdays, Weekends, Weekly, Bi-weekly, Monthly, Yearly, Custom
    val selectedWeekdays: Set<Int> = emptySet(), // Calendar.MONDAY, etc.
    val monthlyPattern: String = "None", // First Monday, Second Tuesday, Third Friday, Last Sunday, etc.
    val endCondition: String = "Never", // Never, Date, Occurrences
    val endConditionDate: Long? = null,
    val endConditionOccurrences: Int = 10
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("mode", mode.name)
        obj.put("startDate", startDate ?: JSONObject.NULL)
        obj.put("endDate", endDate ?: JSONObject.NULL)
        obj.put("intervalDays", intervalDays)
        
        val arr = JSONArray()
        selectedDates.forEach { arr.put(it) }
        obj.put("selectedDates", arr)
        
        obj.put("recurrentPreset", recurrentPreset)
        
        val weekdaysArr = JSONArray()
        selectedWeekdays.forEach { weekdaysArr.put(it) }
        obj.put("selectedWeekdays", weekdaysArr)
        
        obj.put("monthlyPattern", monthlyPattern)
        obj.put("endCondition", endCondition)
        obj.put("endConditionDate", endConditionDate ?: JSONObject.NULL)
        obj.put("endConditionOccurrences", endConditionOccurrences)
        return obj.toString()
    }

    companion object {
        fun fromJson(jsonStr: String?): SchedulerSettings {
            if (jsonStr.isNullOrEmpty()) return SchedulerSettings()
            return try {
                val obj = JSONObject(jsonStr)
                val mode = SchedulingMode.valueOf(obj.optString("mode", SchedulingMode.RANGE.name))
                val startDate = if (obj.isNull("startDate")) null else obj.optLong("startDate")
                val endDate = if (obj.isNull("endDate")) null else obj.optLong("endDate")
                val intervalDays = obj.optInt("intervalDays", 1)
                
                val dates = mutableSetOf<Long>()
                obj.optJSONArray("selectedDates")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        dates.add(arr.getLong(i))
                    }
                }
                
                val recurrentPreset = obj.optString("recurrentPreset", "None")
                
                val weekdays = mutableSetOf<Int>()
                obj.optJSONArray("selectedWeekdays")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        weekdays.add(arr.getInt(i))
                    }
                }
                
                val monthlyPattern = obj.optString("monthlyPattern", "None")
                val endCondition = obj.optString("endCondition", "Never")
                val endConditionDate = if (obj.isNull("endConditionDate")) null else obj.optLong("endConditionDate")
                val endConditionOccurrences = obj.optInt("endConditionOccurrences", 10)
                
                SchedulerSettings(
                    mode = mode,
                    startDate = startDate,
                    endDate = endDate,
                    intervalDays = intervalDays,
                    selectedDates = dates,
                    recurrentPreset = recurrentPreset,
                    selectedWeekdays = weekdays,
                    monthlyPattern = monthlyPattern,
                    endCondition = endCondition,
                    endConditionDate = endConditionDate,
                    endConditionOccurrences = endConditionOccurrences
                )
            } catch (e: Exception) {
                SchedulerSettings()
            }
        }
    }
}

// Utility to clear time from Milliseconds to midnight
fun getMidnightTimeMs(timeMs: Long): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = timeMs
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSchedulerDialog(
    taskTitle: String,
    initialSettings: SchedulerSettings = SchedulerSettings(),
    onDismiss: () -> Unit,
    onSaveSchedule: (SchedulerSettings, List<Long>) -> Unit
) {
    var mode by remember { mutableStateOf(initialSettings.mode) }
    
    // Calendar Navigation State
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val todayMs = remember { getMidnightTimeMs(System.currentTimeMillis()) }
    
    // Core selection fields
    var startDateMs by remember { mutableStateOf(initialSettings.startDate ?: todayMs) }
    var endDateMs by remember { mutableStateOf(initialSettings.endDate ?: (todayMs + 10 * 24 * 60 * 60 * 1000L)) }
    var intervalDays by remember { mutableStateOf(initialSettings.intervalDays) }
    var selectedDates by remember { mutableStateOf(initialSettings.selectedDates) }
    
    // Recurrence fields
    var recurrentPreset by remember { mutableStateOf(initialSettings.recurrentPreset) }
    var selectedWeekdays by remember { mutableStateOf(initialSettings.selectedWeekdays) }
    var monthlyPattern by remember { mutableStateOf(initialSettings.monthlyPattern) }
    
    // End Conditions fields
    var endCondition by remember { mutableStateOf(initialSettings.endCondition) }
    var endConditionDateMs by remember { mutableStateOf(initialSettings.endConditionDate ?: (todayMs + 30 * 24 * 60 * 60 * 1000L)) }
    var endConditionOccurrences by remember { mutableStateOf(initialSettings.endConditionOccurrences) }
    
    // State of selected month and year for jump option
    var showJumpDialog by remember { mutableStateOf(false) }

    // Validation State
    var validationError by remember { mutableStateOf<String?>(null) }

    // Derived: Calculated schedule instances (live recalculation)
    val calculatedDatesList = remember(
        mode, startDateMs, endDateMs, intervalDays, selectedDates,
        recurrentPreset, selectedWeekdays, monthlyPattern, endCondition, endConditionDateMs, endConditionOccurrences
    ) {
        calculateScheduledDates(
            mode = mode,
            startDate = startDateMs,
            endDate = endDateMs,
            intervalDays = intervalDays,
            selectedDates = selectedDates,
            recurrentPreset = recurrentPreset,
            selectedWeekdays = selectedWeekdays,
            monthlyPattern = monthlyPattern,
            endCondition = endCondition,
            endConditionDate = endConditionDateMs,
            endConditionOccurrences = endConditionOccurrences
        )
    }

    // Trigger validation on state updates
    LaunchedEffect(mode, startDateMs, endDateMs, intervalDays, selectedDates, recurrentPreset) {
        validationError = when {
            mode == SchedulingMode.RANGE && endDateMs < startDateMs -> {
                "End Date cannot be before Start Date."
            }
            mode == SchedulingMode.RANGE && intervalDays < 1 -> {
                "Interval must be at least 1 day."
            }
            mode == SchedulingMode.MULTI && selectedDates.isEmpty() -> {
                "Please select at least one date on the calendar."
            }
            calculatedDatesList.isEmpty() -> {
                "Schedule generates 0 dates. Verify settings."
            }
            else -> null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .testTag("scheduler_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Interactive Calendar Schedule",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Task: $taskTitle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_scheduler_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "Close Scheduler")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable visual content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // TAB SELECTOR FOR SCHEDULING MODE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SchedulingMode.values().forEach { schedulingMode ->
                            val isSelected = mode == schedulingMode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { mode = schedulingMode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (schedulingMode) {
                                        SchedulingMode.RANGE -> "Date Range"
                                        SchedulingMode.MULTI -> "Multiple Dates"
                                        SchedulingMode.PATTERN -> "Recurring Pattern"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // CALENDAR GRID & CONTROLS SECTION
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = borderStroke()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Month Navigation Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { showJumpDialog = true }
                                ) {
                                    val formattedMonth = remember(calendarMonth) {
                                        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendarMonth.time)
                                    }
                                    Text(
                                        text = formattedMonth,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Jump To", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            calendarMonth = Calendar.getInstance()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Today, contentDescription = "Return Today", modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val clone = calendarMonth.clone() as Calendar
                                            clone.add(Calendar.MONTH, -1)
                                            calendarMonth = clone
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                                    }

                                    IconButton(
                                        onClick = {
                                            val clone = calendarMonth.clone() as Calendar
                                            clone.add(Calendar.MONTH, 1)
                                            calendarMonth = clone
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Grid Composable
                            InteractiveSchedulerCalendarGrid(
                                calendarMonth = calendarMonth,
                                todayMs = todayMs,
                                mode = mode,
                                startDate = startDateMs,
                                endDate = endDateMs,
                                intervalDays = intervalDays,
                                selectedDates = selectedDates,
                                activeDates = calculatedDatesList.toSet(),
                                onDateClick = { clickedMs ->
                                    when (mode) {
                                        SchedulingMode.RANGE -> {
                                            // Smart range selection toggle
                                            if (clickedMs <= startDateMs) {
                                                startDateMs = clickedMs
                                            } else {
                                                endDateMs = clickedMs
                                            }
                                        }
                                        SchedulingMode.MULTI -> {
                                            selectedDates = if (selectedDates.contains(clickedMs)) {
                                                selectedDates - clickedMs
                                            } else {
                                                selectedDates + clickedMs
                                            }
                                        }
                                        SchedulingMode.PATTERN -> {
                                            startDateMs = clickedMs
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // SCHEDULER CONTROLS BASED ON SELECT MODE
                    AnimatedContent(targetState = mode, label = "ModeControls") { targetMode ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            when (targetMode) {
                                SchedulingMode.RANGE -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            "Interval Options (Alternate Days)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                Pair("Every Day", 1),
                                                Pair("Every 2 Days", 2),
                                                Pair("Every 3 Days", 3),
                                                Pair("Every 4 Days", 4)
                                            ).forEach { pair ->
                                                val isSel = intervalDays == pair.second
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                        .border(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(8.dp))
                                                        .clickable { intervalDays = pair.second }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(pair.first, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }

                                        // Custom Interval Spinner/Field
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("Or Custom Interval:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 4.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { if (intervalDays > 1) intervalDays-- },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Decrement", modifier = Modifier.size(16.dp))
                                                }
                                                Text(
                                                    text = "$intervalDays Days",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )
                                                IconButton(
                                                    onClick = { intervalDays++ },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Increment", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }

                                SchedulingMode.MULTI -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Manual Selection",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Tap on any date above to toggle scheduling on or off. Multi-selected dates are marked with dark circles.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Button(
                                            onClick = { selectedDates = emptySet() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                        ) {
                                            Text("Clear Selection", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                        }
                                    }
                                }

                                SchedulingMode.PATTERN -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            "Recurrence Patterns & Rules",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )

                                        // Presets
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(
                                                "Daily", "Weekdays only", "Weekends only", 
                                                "Weekly", "Bi-weekly", "Monthly", "Yearly"
                                            ).forEach { preset ->
                                                val isSel = recurrentPreset == preset
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                        .clickable { 
                                                            recurrentPreset = preset 
                                                            // Auto-configure weekdays for Weekdays/Weekends presets
                                                            if (preset == "Weekdays only") {
                                                                selectedWeekdays = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
                                                            } else if (preset == "Weekends only") {
                                                                selectedWeekdays = setOf(Calendar.SATURDAY, Calendar.SUNDAY)
                                                            } else {
                                                                selectedWeekdays = emptySet()
                                                            }
                                                            monthlyPattern = "None"
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(preset, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }

                                        // Weekdays selections
                                        Text("Select Day-of-Week:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val days = listOf(
                                                Pair("M", Calendar.MONDAY),
                                                Pair("T", Calendar.TUESDAY),
                                                Pair("W", Calendar.WEDNESDAY),
                                                Pair("T", Calendar.THURSDAY),
                                                Pair("F", Calendar.FRIDAY),
                                                Pair("S", Calendar.SATURDAY),
                                                Pair("S", Calendar.SUNDAY)
                                            )
                                            days.forEach { pair ->
                                                val isChecked = selectedWeekdays.contains(pair.second)
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                                        .clickable {
                                                            selectedWeekdays = if (isChecked) {
                                                                selectedWeekdays - pair.second
                                                            } else {
                                                                selectedWeekdays + pair.second
                                                            }
                                                            recurrentPreset = "Custom Weekdays"
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = pair.first,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isChecked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }

                                        // Monthly Pattern Selector
                                        Text("Monthly Recurring Pattern:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf("First Monday", "Second Tuesday", "Third Friday", "Last Sunday").forEach { pattern ->
                                                val isSel = monthlyPattern == pattern
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                        .clickable { 
                                                            monthlyPattern = pattern
                                                            recurrentPreset = "Monthly Pattern"
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(pattern, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        }

                                        // Repeat End Condition (Rule 8)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            "Flexible End Condition",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf("Never", "On Date", "After Occurrences").forEach { cond ->
                                                val isSel = endCondition == cond
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.clickable { endCondition = cond }
                                                ) {
                                                    RadioButton(selected = isSel, onClick = { endCondition = cond })
                                                    Text(cond, fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        AnimatedVisibility(visible = endCondition == "On Date") {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("End Date: ", fontSize = 11.sp)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .clickable {
                                                            // Custom picker can just toggle forward 30 days easily for prototype
                                                            endConditionDateMs += 10 * 24 * 60 * 60 * 1000L
                                                        }
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                                    Text(sdf.format(Date(endConditionDateMs)), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text(" (Tap to add 10 days)", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp))
                                            }
                                        }

                                        AnimatedVisibility(visible = endCondition == "After Occurrences") {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text("End After:", fontSize = 11.sp)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                ) {
                                                    IconButton(onClick = { if (endConditionOccurrences > 1) endConditionOccurrences-- }) {
                                                        Icon(Icons.Default.Remove, "Decrement", modifier = Modifier.size(16.dp))
                                                    }
                                                    Text("$endConditionOccurrences Occurrences", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    IconButton(onClick = { endConditionOccurrences++ }) {
                                                        Icon(Icons.Default.Add, "Increment", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SCHEDULER SUMMARY (Requirement 11)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = borderStroke()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Live Schedule Summary",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            val summarySdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                            
                            val rangeStr = when (mode) {
                                SchedulingMode.RANGE -> {
                                    "${summarySdf.format(Date(startDateMs))} → ${summarySdf.format(Date(endDateMs))}"
                                }
                                SchedulingMode.PATTERN -> {
                                    val endStr = when (endCondition) {
                                        "Never" -> "No End Date"
                                        "On Date" -> "Ends ${summarySdf.format(Date(endConditionDateMs))}"
                                        else -> "Ends after $endConditionOccurrences occurrences"
                                    }
                                    "Starts ${summarySdf.format(Date(startDateMs))} ($endStr)"
                                }
                                SchedulingMode.MULTI -> {
                                    "Manually Selected Dates"
                                }
                            }

                            val patternSummary = when (mode) {
                                SchedulingMode.RANGE -> {
                                    if (intervalDays == 1) "Every day" else "Every $intervalDays Days"
                                }
                                SchedulingMode.PATTERN -> {
                                    val wStr = if (selectedWeekdays.isNotEmpty()) {
                                        "Days: " + selectedWeekdays.joinToString { 
                                            when(it) {
                                                Calendar.MONDAY -> "Mon"
                                                Calendar.TUESDAY -> "Tue"
                                                Calendar.WEDNESDAY -> "Wed"
                                                Calendar.THURSDAY -> "Thu"
                                                Calendar.FRIDAY -> "Fri"
                                                Calendar.SATURDAY -> "Sat"
                                                Calendar.SUNDAY -> "Sun"
                                                else -> ""
                                            }
                                        }
                                    } else ""
                                    
                                    val pStr = if (monthlyPattern != "None") "Pattern: $monthlyPattern" else ""
                                    
                                    listOf(recurrentPreset, wStr, pStr).filter { it.isNotEmpty() }.joinToString(" • ")
                                }
                                SchedulingMode.MULTI -> "No general pattern"
                            }

                            val datesSplit = calculatedDatesList.take(6).joinToString(", ") {
                                val cal = Calendar.getInstance().apply { timeInMillis = it }
                                cal.get(Calendar.DAY_OF_MONTH).toString()
                            }
                            val datesSuffix = if (calculatedDatesList.size > 6) "..." else ""

                            // Text presentation
                            Text("Schedule Range: \n$rangeStr", fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 2.dp))
                            Text("Pattern Settings: $patternSummary", fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 2.dp))
                            Text("Calculated Occurrences: ${datesSplit}${datesSuffix}", fontSize = 10.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 2.dp))
                            
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Total: ${calculatedDatesList.size} scheduled tasks",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Show validation messages elegantly (Rule 13)
                    validationError?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, "Error", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(err, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_action_btn")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (validationError == null) {
                                val currentSettings = SchedulerSettings(
                                    mode = mode,
                                    startDate = startDateMs,
                                    endDate = endDateMs,
                                    intervalDays = intervalDays,
                                    selectedDates = selectedDates,
                                    recurrentPreset = recurrentPreset,
                                    selectedWeekdays = selectedWeekdays,
                                    monthlyPattern = monthlyPattern,
                                    endCondition = endCondition,
                                    endConditionDate = endConditionDateMs,
                                    endConditionOccurrences = endConditionOccurrences
                                )
                                onSaveSchedule(currentSettings, calculatedDatesList)
                            }
                        },
                        enabled = (validationError == null),
                        modifier = Modifier.testTag("save_schedule_btn")
                    ) {
                        Text("Apply Schedule")
                    }
                }
            }
        }
    }

    // Month Selector dialog (Requirement 9)
    if (showJumpDialog) {
        Dialog(onDismissRequest = { showJumpDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Target Month", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    
                    // Simple grid of months
                    val months = listOf(
                        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0..3) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (col in 0..2) {
                                    val index = row * 3 + col
                                    val mName = months[index]
                                    val calClone = calendarMonth.clone() as Calendar
                                    val isCurrent = calClone.get(Calendar.MONTH) == index
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                val c = calendarMonth.clone() as Calendar
                                                c.set(Calendar.MONTH, index)
                                                calendarMonth = c
                                                showJumpDialog = false
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(mName, fontWeight = FontWeight.Bold, color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Jump Years
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Year:", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(2026, 2027, 2028).forEach { yr ->
                                val isYr = calendarMonth.get(Calendar.YEAR) == yr
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isYr) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            val c = calendarMonth.clone() as Calendar
                                            c.set(Calendar.YEAR, yr)
                                            calendarMonth = c
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(yr.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isYr) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Function to calculate all schedule dates based on mode, interval, and recurrence preset (Requirement 11)
fun calculateScheduledDates(
    mode: SchedulingMode,
    startDate: Long,
    endDate: Long,
    intervalDays: Int,
    selectedDates: Set<Long>,
    recurrentPreset: String,
    selectedWeekdays: Set<Int>,
    monthlyPattern: String,
    endCondition: String,
    endConditionDate: Long?,
    endConditionOccurrences: Int
): List<Long> {
    val output = mutableListOf<Long>()
    when (mode) {
        SchedulingMode.RANGE -> {
            val startCal = Calendar.getInstance().apply { timeInMillis = getMidnightTimeMs(startDate) }
            val endCal = Calendar.getInstance().apply { timeInMillis = getMidnightTimeMs(endDate) }
            
            // Generate in safe loop
            val maxIterations = 365
            var currentIter = 0
            while (!startCal.after(endCal) && currentIter < maxIterations) {
                output.add(startCal.timeInMillis)
                startCal.add(Calendar.DAY_OF_YEAR, intervalDays)
                currentIter++
            }
        }
        
        SchedulingMode.MULTI -> {
            output.addAll(selectedDates.sorted())
        }
        
        SchedulingMode.PATTERN -> {
            val startCal = Calendar.getInstance().apply { timeInMillis = getMidnightTimeMs(startDate) }
            
            // Setup bounds
            val stopCal = Calendar.getInstance()
            if (endCondition == "On Date" && endConditionDate != null) {
                stopCal.timeInMillis = getMidnightTimeMs(endConditionDate)
            } else {
                // Pre-configure general boundary to prevent infinite loop (1 year)
                stopCal.timeInMillis = startCal.timeInMillis + 365L * 24 * 60 * 60 * 1000L
            }

            var count = 0
            val limit = if (endCondition == "After Occurrences") endConditionOccurrences else 100
            val maxIterations = 730
            var iterations = 0

            while (iterations < maxIterations && count < limit && !startCal.after(stopCal)) {
                iterations++
                
                // 1. Specific conditions for Recurrent options and Day of Week
                val dow = startCal.get(Calendar.DAY_OF_WEEK)
                val isMatchingDow = selectedWeekdays.isEmpty() || selectedWeekdays.contains(dow)
                
                // Check monthly patterns
                var monthlyPatternMatch = true
                if (monthlyPattern != "None") {
                    monthlyPatternMatch = isDateMatchingPattern(startCal, monthlyPattern)
                }

                var match = false
                when (recurrentPreset) {
                    "Daily" -> match = true
                    "Weekdays only" -> match = (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY)
                    "Weekends only" -> match = (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY)
                    "Weekly" -> match = isMatchingDow
                    "Bi-weekly" -> {
                        // Match day of week every alternate week
                        val weekNo = startCal.get(Calendar.WEEK_OF_YEAR)
                        match = isMatchingDow && (weekNo % 2 == 0)
                    }
                    "Monthly" -> {
                        // Match same day of the month
                        val specDay = Calendar.getInstance().apply { timeInMillis = startDate }.get(Calendar.DAY_OF_MONTH)
                        match = startCal.get(Calendar.DAY_OF_MONTH) == specDay
                    }
                    "Monthly Pattern" -> {
                        match = monthlyPatternMatch
                    }
                    "Yearly" -> {
                        // Match same month and day
                        val specCal = Calendar.getInstance().apply { timeInMillis = startDate }
                        match = startCal.get(Calendar.MONTH) == specCal.get(Calendar.MONTH) &&
                                startCal.get(Calendar.DAY_OF_MONTH) == specCal.get(Calendar.DAY_OF_MONTH)
                    }
                    "Custom Weekdays" -> match = isMatchingDow
                    else -> match = true // Defaults to matching simple range/start list
                }

                if (match) {
                    output.add(startCal.timeInMillis)
                    count++
                }

                // Increment day by day
                startCal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }
    return output
}

// Monthly pattern matches: "First Monday", "Second Tuesday", "Third Friday", "Last Sunday"
fun isDateMatchingPattern(cal: Calendar, pattern: String): Boolean {
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    
    val weekInMonth = cal.get(Calendar.DAY_OF_WEEK_IN_MONTH) // 1 for first week, 2 for second...
    val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val isLastWeek = (dayOfMonth + 7) > totalDays

    return when (pattern) {
        "First Monday" -> dayOfWeek == Calendar.MONDAY && weekInMonth == 1
        "Second Tuesday" -> dayOfWeek == Calendar.TUESDAY && weekInMonth == 2
        "Third Friday" -> dayOfWeek == Calendar.FRIDAY && weekInMonth == 3
        "Last Sunday" -> dayOfWeek == Calendar.SUNDAY && isLastWeek
        else -> false
    }
}

@Composable
fun InteractiveSchedulerCalendarGrid(
    calendarMonth: Calendar,
    todayMs: Long,
    mode: SchedulingMode,
    startDate: Long,
    endDate: Long,
    intervalDays: Int,
    selectedDates: Set<Long>,
    activeDates: Set<Long>,
    onDateClick: (Long) -> Unit
) {
    val daysInMonth = calendarMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayCal = calendarMonth.clone() as Calendar
    firstDayCal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK)
    val blankPrefixes = firstDayOfWeek - 1

    val weekDays = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    Column(modifier = Modifier.fillMaxWidth()) {
        // Week Header
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { dayName ->
                Text(
                    text = dayName,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        var currentDay = 1
        for (row in 0..5) {
            if (currentDay > daysInMonth) break
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                for (col in 1..7) {
                    val index = row * 7 + col
                    val isBlank = index <= blankPrefixes || currentDay > daysInMonth

                    if (isBlank) {
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        val dayValue = currentDay
                        val dayCal = calendarMonth.clone() as Calendar
                        dayCal.set(Calendar.DAY_OF_MONTH, dayValue)
                        val dayMs = getMidnightTimeMs(dayCal.timeInMillis)

                        val isToday = dayMs == todayMs
                        val isActive = activeDates.contains(dayMs)
                        
                        // Check if day matches boundaries
                        val isStart = (mode == SchedulingMode.RANGE && dayMs == startDate) || (mode == SchedulingMode.PATTERN && dayMs == startDate)
                        val isEnd = (mode == SchedulingMode.RANGE && dayMs == endDate)
                        
                        val isSelectedMulti = mode == SchedulingMode.MULTI && selectedDates.contains(dayMs)
                        val isBetweenRange = mode == SchedulingMode.RANGE && dayMs > startDate && dayMs < endDate

                        // Cell backgrounds based on range, actives, today
                        val cellBg = when {
                            isStart || isEnd -> MaterialTheme.colorScheme.primary
                            isSelectedMulti -> MaterialTheme.colorScheme.secondary
                            isBetweenRange -> {
                                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            }
                            isActive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4e-1f) // subtle tint
                            isToday -> MaterialTheme.colorScheme.surfaceVariant
                            else -> Color.Transparent
                        }

                        // Text color
                        val txtColor = when {
                            isStart || isEnd -> MaterialTheme.colorScheme.onPrimary
                            isSelectedMulti -> MaterialTheme.colorScheme.onSecondary
                            isBetweenRange -> MaterialTheme.colorScheme.onPrimaryContainer
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBg)
                                .clickable { onDateClick(dayMs) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = dayValue.toString(),
                                    fontWeight = if (isToday || isStart || isEnd || isActive) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = txtColor
                                )
                                
                                // Extra indicators (Rule 10)
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(if (isStart || isEnd) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                                            .padding(top = 1.dp)
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

@Composable
fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
)
