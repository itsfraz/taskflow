package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CategoryEntity
import com.example.data.TaskEntity
import com.example.ui.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TaskViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFocus: () -> Unit
) {
    val context = LocalContext.current
    val tasks by viewModel.filteredTasks.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val stats = viewModel.getTaskStats()

    var showQuickAddSheet by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showSmartSuggestionDialog by remember { mutableStateOf(false) }
    var isStatsExpanded by remember { mutableStateOf(false) }

    // Search & Filter state bindings
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterTimeframe by viewModel.filterTimeframe.collectAsState()
    val filterCategory by viewModel.filterCategory.collectAsState()
    val filterPriority by viewModel.filterPriority.collectAsState()
    val selectedTaskIds by viewModel.selectedTaskIds.collectAsState()

    // Setup snackbar host
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Top Custom Header Row matching Jordan's Sleek Dashboard Style - Compacted
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val greeting = "Hi, Jordan! 👋"
                    Column {
                        val df = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
                        Text(
                            text = df.format(Date()).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = greeting,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Compact AI Smart Suggestions Button
                        FilledTonalButton(
                            onClick = { showSmartSuggestionDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("get_ai_suggestions_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Suggestions",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        // Compact Person Avatar Badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Search Bar with Integrated Filters
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search tasks...", fontSize = 13.sp) },
                    textStyle = TextStyle(fontSize = 13.sp),
                    leadingIcon = { Icon(Icons.Default.Search, "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    modifier = Modifier.size(32.dp),
                                    onClick = { viewModel.searchQuery.value = "" }
                                ) {
                                    Icon(Icons.Default.Clear, "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(
                                modifier = Modifier.size(32.dp),
                                onClick = { showVoiceDialog = true }
                            ) {
                                Icon(Icons.Outlined.Mic, "Voice Quick Task", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("search_bar")
                )
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Home tab
                        BottomNavItem(
                            icon = Icons.Default.Home,
                            label = "Home",
                            isSelected = true,
                            onClick = {}
                        )

                        // Calendar tab
                        BottomNavItem(
                            icon = Icons.Default.CalendarToday,
                            label = "Calendar",
                            isSelected = false,
                            onClick = onNavigateToCalendar
                        )

                        // Integrated prominent Add Task FAB in the footer
                        FloatingActionButton(
                            onClick = { showQuickAddSheet = true },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("add_task_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(28.dp))
                        }

                        // Focus Mode tab
                        BottomNavItem(
                            icon = Icons.Default.HourglassEmpty,
                            label = "Focus",
                            isSelected = false,
                            testTag = "focus_mode_tab_btn",
                            onClick = onNavigateToFocus
                        )

                        // Stats tab
                        BottomNavItem(
                            icon = Icons.Default.BarChart,
                            label = "Stats",
                            isSelected = false,
                            onClick = onNavigateToStats
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Modern, responsive horizontal scroll line for Timeframe filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val timeframes = listOf(
                    "Today" to "Today",
                    "Upcoming" to "Upcoming",
                    "Completed" to "Completed",
                    "Overdue" to "Overdue",
                    "All" to "All Tasks"
                )
                timeframes.forEach { (tfKey, tfLabel) ->
                    val isSelected = filterTimeframe == tfKey
                    val containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    }
                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val borderStroke = if (isSelected) {
                        null
                    } else {
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }

                    Surface(
                        onClick = { viewModel.filterTimeframe.value = tfKey },
                        shape = RoundedCornerShape(20.dp),
                        color = containerColor,
                        contentColor = contentColor,
                        border = borderStroke,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("timeframe_filter_chip_$tfKey")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = tfLabel,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Consolidated, ultra-compact scroll line for Category filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAllCatSelected = filterCategory == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isAllCatSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable { viewModel.filterCategory.value = null }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "All",
                        fontSize = 11.sp,
                        fontWeight = if (isAllCatSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAllCatSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                for (cat in categories) {
                    val isSelected = filterCategory == cat.name
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { viewModel.filterCategory.value = cat.name }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(cat.colorHex)))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                cat.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!cat.isSystem) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete",
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clickable { viewModel.deleteCategory(cat.id) },
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                // Add Category compact button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .clickable { showCategoryDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, "Add Category", modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            // Expandable/collapsible Productivity Report Insight Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isStatsExpanded = !isStatsExpanded }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Stats",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Productivity Insight (${stats.completionRate}%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isStatsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isStatsExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (isStatsExpanded) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    ProductivityBannerCard(stats = stats)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Bulk actions banner if items are selected
            if (selectedTaskIds.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${selectedTaskIds.size} Selected",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.bulkComplete(context, true) }) {
                                Text("Complete")
                            }
                            TextButton(onClick = { viewModel.bulkDelete(context) }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { viewModel.clearSelections() }) {
                                Icon(Icons.Default.Close, "Cancel Selection")
                            }
                        }
                    }
                }
            }

            // Task List
            if (tasks.isEmpty()) {
                val (emptyIcon, emptyTitle, emptyDesc) = when (filterTimeframe) {
                    "Today" -> Triple(
                        Icons.Default.AssignmentTurnedIn,
                        "No tasks scheduled for today",
                        "Enjoy your day or create a new task!"
                    )
                    "Upcoming" -> Triple(
                        Icons.Default.CalendarToday,
                        "No upcoming tasks",
                        "Your schedule is completely clear! Click + to add clean tasks."
                    )
                    "Completed" -> Triple(
                        Icons.Default.CheckCircle,
                        "No completed tasks yet",
                        "Complete your pending tasks to see them stack up here!"
                    )
                    "Overdue" -> Triple(
                        Icons.Default.Info,
                        "No overdue tasks",
                        "Fantastic! You are fully on track with your schedule."
                    )
                    else -> Triple(
                        Icons.Default.AssignmentTurnedIn,
                        "No tasks found",
                        "Tap the + button to create a smart task with Natural Language!"
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 60.dp)
                    ) {
                        Icon(
                            imageVector = emptyIcon,
                            contentDescription = emptyTitle,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = emptyTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = emptyDesc,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            isSelected = selectedTaskIds.contains(task.id),
                            onTap = {
                                if (selectedTaskIds.isNotEmpty()) {
                                    viewModel.toggleTaskSelection(task.id)
                                } else {
                                    onNavigateToDetail(task.id)
                                }
                            },
                            onLongTap = { viewModel.toggleTaskSelection(task.id) },
                            onCheckedChange = { viewModel.toggleTaskCompletion(context, task) },
                            onPinToggle = { viewModel.toggleTaskPin(task) },
                            onFavoriteToggle = { viewModel.toggleTaskFavorite(task) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add Category Alert Dialog
    if (showCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        val catColors = listOf("#F44336", "#E91E63", "#9C27B0", "#673AB7", "#3F51B5", "#2196F3", "#00BCD4", "#4CAF50", "#FFEB3B", "#FF9800")
        var selectedColor by remember { mutableStateOf(catColors[0]) }

        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Add Custom Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Select Color", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        catColors.forEach { color ->
                            val parsed = Color(android.graphics.Color.parseColor(color))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(
                                        width = if (selectedColor == color) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addCategory(catName, "label", selectedColor)
                        showCategoryDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSmartSuggestionDialog) {
        SmartSuggestionDialog(
            viewModel = viewModel,
            existingTasks = viewModel.allTasks.value,
            onDismiss = { showSmartSuggestionDialog = false }
        )
    }

    // Voice Quick Command Simulation Fallback Dialog
    if (showVoiceDialog) {
        var voiceCommandText by remember { mutableStateOf("") }
        var voiceResultMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            title = { Text("Voice Input / Quick Add Launcher") },
            text = {
                Column {
                    Text(
                        "Type or speak a smart natural language command to add tasks instantly:\n" +
                        "e.g., 'add finish slides due tomorrow priority high'",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = voiceCommandText,
                        onValueChange = { voiceCommandText = it },
                        label = { Text("Natural Language Command") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (voiceResultMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(voiceResultMsg!!, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                var isParsing by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isParsing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Button(
                        enabled = !isParsing,
                        onClick = {
                            isParsing = true
                            viewModel.parseVoiceCommandAi(context, voiceCommandText) { errorMsg ->
                                isParsing = false
                                if (errorMsg == null) {
                                    showVoiceDialog = false
                                    android.widget.Toast.makeText(context, "Added smart task via AI successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    voiceResultMsg = errorMsg
                                }
                            }
                        }
                    ) {
                        Text(if (isParsing) "Parsing AI..." else "Import Task")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Simple sheet like modal to create a Task
    if (showQuickAddSheet) {
        QuickAddTaskDialog(
            viewModel = viewModel,
            categories = categories,
            onDismiss = { showQuickAddSheet = false }
        )
    }
}

@Composable
fun ProductivityBannerCard(stats: com.example.ui.TaskStats) {
    Card(
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF005FB0), Color(0xFF0D47A1))
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Goal",
                        color = Color(0xFFD4E3FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                          text = "${stats.completionRate}",
                          color = Color.White,
                          fontSize = 32.sp,
                          fontWeight = FontWeight.Bold
                        )
                        Text(
                          text = "%",
                          color = Color(0xFFD4E3FF),
                          fontSize = 18.sp,
                          fontWeight = FontWeight.Medium,
                          modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${stats.completed} of ${stats.total} tasks completed",
                        color = Color(0xFFD4E3FF),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Streak: ${stats.currentStreak} days",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp)
                ) {
                    val percentAnimate = animateFloatAsState(
                        targetValue = stats.completionRate / 100f,
                        label = "Progress"
                    ).value

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 6.dp.toPx()
                        drawCircle(
                            color = Color(0xFF0D47A1).copy(alpha = 0.4f),
                            style = Stroke(width = strokeWidth)
                        )
                        drawArc(
                            color = Color.White,
                            startAngle = -90f,
                            sweepAngle = 360f * percentAnimate,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                    Text(
                        text = "${stats.completionRate}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItemCard(
    task: TaskEntity,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongTap: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onPinToggle: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val context = LocalContext.current
    
    // Smooth tap/press animations & physical tactile feedback
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val animatedElevation by animateDpAsState(
        targetValue = if (isPressed) 6.dp else 2.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "elevation"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )

    // Determine status and overdue states
    val isOverdue = remember(task) {
        if (task.isCompleted || task.isArchived) false
        else {
            task.dueDate?.let { dueDate ->
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                dueDate < todayStart
            } ?: false
        }
    }

    val rowBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val borderStroke = if (isSelected) {
        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = rowBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onTap,
                onLongClick = onLongTap
            )
            .testTag("task_item_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Accessible Custom Circular Checkbox Touch Target (48dp x 48dp)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onCheckedChange(!task.isCompleted) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.isCompleted) MaterialTheme.colorScheme.primary 
                            else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Main Content Area
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                // Category & Title Area
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge (Notion Style)
                    if (task.category.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = task.category,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    if (task.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                val titleColor = if (task.isCompleted) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }

                // Promo title
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textDecoration = textDecoration,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                if (task.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        fontSize = 13.sp,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom badge/meta area: Responsive, beautifully spaced
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Priority Badge
                    val (priorityBg, priorityFg, priorityIcon) = when (task.priority.lowercase()) {
                        "urgent" -> Triple(Color(0xFFFFDAD6), Color(0xFFBA1A1A), Icons.Default.Warning)
                        "high" -> Triple(Color(0xFFFFDAD6), Color(0xFFBA1A1A), Icons.Default.ArrowUpward)
                        "medium" -> Triple(Color(0xFFFFF1C5), Color(0xFF7A5900), Icons.Default.ArrowForward)
                        else -> Triple(Color(0xFFE2F1FF), Color(0xFF0F5A82), Icons.Default.ArrowDownward) // Low
                    }

                    Surface(
                        color = priorityBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = priorityIcon,
                                contentDescription = "Priority",
                                tint = priorityFg,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.priority.uppercase(),
                                fontSize = 9.sp,
                                color = priorityFg,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Status Badge (Pending / Completed / Overdue)
                    val (statusBg, statusFg, statusLabel) = when {
                        task.isCompleted -> Triple(Color(0xFFE2F6EA), Color(0xFF147E43), "COMPLETED")
                        isOverdue -> Triple(Color(0xFFFFEAEA), Color(0xFFD32F2F), "OVERDUE")
                        else -> Triple(Color(0xFFF0F1F5), Color(0xFF5F6368), "PENDING")
                    }

                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    }

                    // Due Time & Icon
                    if (task.dueDate != null) {
                        val dateText = remember(task.dueDate, task.dueTime) {
                            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(task.dueDate))
                            val timeStr = task.dueTime?.let { " $it" } ?: ""
                            "$dateStr$timeStr"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Due Date",
                                modifier = Modifier.size(11.dp),
                                tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dateText,
                                fontSize = 11.sp,
                                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Quick actions on the right (Pin, Favorite), nicely columns
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                IconButton(
                    onClick = onPinToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin Task",
                        tint = if (task.isPinned) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (task.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Task",
                        tint = if (task.isFavorite) {
                            Color.Red
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAddTaskDialog(
    viewModel: TaskViewModel,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.name ?: "Personal") }
    var priority by remember { mutableStateOf("Medium") }
    
    // Date & Time pickers fields
    var selectedDateMs by remember { mutableStateOf<Long?>(null) }
    var dueTime by remember { mutableStateOf("") }
    var reminderOffsetMin by remember { mutableStateOf(15) } // Default 15 mins
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Advanced interactive schedule states
    var schedulerSettings by remember { mutableStateOf(SchedulerSettings()) }
    var scheduledDatesList by remember { mutableStateOf<List<Long>>(emptyList()) }
    var showSchedulerDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Smart Task") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_task_title"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Priority drop/selection
                Text("Priority Level", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Low", "Medium", "High", "Urgent").forEach { p ->
                        val isSel = priority == p
                        ElevatedFilterChip(
                            selected = isSel,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive, advanced calendar task scheduler
                Text("Task Schedule", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                
                if (scheduledDatesList.isNotEmpty()) {
                    // Visual confirmation card of customized schedule
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSchedulerDialog = true }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Configured Schedule",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Schedule Configured",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${scheduledDatesList.size} occurrences scheduled. Tap to edit.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Schedule",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    // Default basic picker that prompts to configure schedule
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { 
                                selectedDateMs = System.currentTimeMillis() 
                                scheduledDatesList = emptyList() // clear advanced
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedDateMs != null && scheduledDatesList.isEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Today", fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                selectedDateMs = cal.timeInMillis
                                scheduledDatesList = emptyList() // clear advanced
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedDateMs != null && scheduledDatesList.isEmpty()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("Tomorrow", fontSize = 11.sp)
                        }
                        
                        Button(
                            onClick = { showSchedulerDialog = true },
                            modifier = Modifier.weight(1.2f).testTag("choose_advanced_schedule_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Tune, "Advanced Settings", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Customize", fontSize = 11.sp)
                        }
                    }
                }

                // Call custom Dialog
                if (showSchedulerDialog) {
                    TaskSchedulerDialog(
                        taskTitle = if (title.isEmpty()) "Untitled Task" else title,
                        initialSettings = schedulerSettings,
                        onDismiss = { showSchedulerDialog = false },
                        onSaveSchedule = { settings, dates ->
                            schedulerSettings = settings
                            scheduledDatesList = dates
                            showSchedulerDialog = false
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category List Choose
                Text("Category Code", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat.name,
                            onClick = { selectedCategory = cat.name },
                            label = { Text(cat.name) }
                        )
                    }
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.trim().isEmpty()) {
                        errorMsg = "Task title cannot be empty"
                    } else {
                        if (scheduledDatesList.isNotEmpty()) {
                            val groupId = "group_${System.currentTimeMillis()}"
                            viewModel.insertScheduledTasks(
                                context = context,
                                title = title,
                                description = description,
                                category = selectedCategory,
                                priority = priority,
                                colorLabel = "#2196F3",
                                dueTime = if (dueTime.isEmpty()) "12:00" else dueTime,
                                reminderOffsetMin = reminderOffsetMin,
                                dates = scheduledDatesList,
                                scheduleGroupId = groupId,
                                scheduleConfigJson = schedulerSettings.toJson()
                            )
                        } else {
                            val error = viewModel.insertTaskWithContext(
                                context = context,
                                title = title,
                                description = description,
                                category = selectedCategory,
                                priority = priority,
                                dueDate = selectedDateMs ?: System.currentTimeMillis(),
                                dueTime = if (dueTime.isEmpty()) "12:00" else dueTime,
                                reminderOffsetMin = reminderOffsetMin,
                                colorLabel = "#2196F3"
                            )
                            if (error != null) {
                                errorMsg = error
                                return@Button
                            }
                        }
                        onDismiss()
                    }
                }
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String = "",
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    else Color.Transparent
                )
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

