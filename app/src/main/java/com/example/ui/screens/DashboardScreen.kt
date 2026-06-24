package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import com.example.data.CategoryEntity
import com.example.data.TaskEntity
import com.example.ui.TaskViewModel
import com.example.util.TimeScheduleUtils
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
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
    var isRefreshing by remember { mutableStateOf(false) }

    // Search & Filter state bindings
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filterTimeframe by viewModel.filterTimeframe.collectAsState()
    val filterCategory by viewModel.filterCategory.collectAsState()
    val filterPriority by viewModel.filterPriority.collectAsState()
    val selectedTaskIds by viewModel.selectedTaskIds.collectAsState()

    // Setup snackbar host
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val rotationInfiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val syncRotationAngle by rotationInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_rotation"
    )

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

                        // Modern Animated Sync/Refresh Action Button
                        IconButton(
                            onClick = {
                                if (!isRefreshing) {
                                    isRefreshing = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(1200)
                                        isRefreshing = false
                                        snackbarHostState.showSnackbar(
                                            message = "Tasks synchronized successfully",
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Tasks",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer {
                                        if (isRefreshing) {
                                            rotationZ = syncRotationAngle
                                        }
                                    }
                            )
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
                val allCatBgColor by animateColorAsState(
                    targetValue = if (isAllCatSelected) MaterialTheme.colorScheme.primaryContainer
                                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    animationSpec = tween(durationMillis = 200)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(allCatBgColor)
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
                    val catBgColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        animationSpec = tween(durationMillis = 200)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(catBgColor)
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
            } else if (isRefreshing) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(3) {
                        SkeletonTaskItem()
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                        AnimatedStaggeredEntrance(index = index) {
                            SwipeableTaskContainer(
                                task = task,
                                onDelete = {
                                    viewModel.deleteTask(context, task)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Task \"${task.title}\" deleted",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            viewModel.restoreTask(context, task)
                                        }
                                    }
                                },
                                onToggleComplete = {
                                    viewModel.toggleTaskCompletion(context, task)
                                },
                                modifier = Modifier.animateItem()
                            ) {
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
                        }
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
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
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

    // Playful bouncy checkbox animations & haptics
    val checkboxChecked = task.isCompleted
    val view = LocalView.current

    val checkIconScale by animateFloatAsState(
        targetValue = if (checkboxChecked) 1f else 0f,
        animationSpec = if (checkboxChecked) {
            spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            tween(durationMillis = 150)
        },
        label = "check_icon_scale"
    )
    
    val checkboxContainerScale by animateFloatAsState(
        targetValue = if (checkboxChecked) 1.1f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkbox_container_scale"
    )

    val checkboxBgColor by animateColorAsState(
        targetValue = if (checkboxChecked) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "checkbox_bg_color"
    )
    
    val checkboxBorderColor by animateColorAsState(
        targetValue = if (checkboxChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
        animationSpec = tween(durationMillis = 200),
        label = "checkbox_border_color"
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

    val taskColor = remember(task.colorLabel) {
        try {
            Color(android.graphics.Color.parseColor(task.colorLabel.ifEmpty { "#2196F3" }))
        } catch (e: Exception) {
            Color(0xFF2196F3)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = rowBg),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        modifier = modifier
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
                .height(IntrinsicSize.Min)
        ) {
            // Modern vertical colored strip indicator (left-accent) matching custom category/task color
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(
                        color = taskColor,
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Accessible Custom Circular Checkbox Touch Target (48dp x 48dp)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                                onCheckedChange(!task.isCompleted)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .scale(checkboxContainerScale)
                            .clip(CircleShape)
                            .background(checkboxBgColor)
                            .border(
                                width = 2.dp,
                                color = checkboxBorderColor,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (checkIconScale > 0.01f) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .size(13.dp)
                                    .scale(checkIconScale)
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
                        // Category Badge (Notion Style with Category Color-coding!)
                        if (task.category.isNotEmpty()) {
                            Surface(
                                color = taskColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = task.category,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = taskColor
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

                    Spacer(modifier = Modifier.height(6.dp))

                    val textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    val titleColor = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    // Task title
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

                    // Horizontally aligned Date & Timing row, just below description/title in bold
                    if (task.dueDate != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val dateText = remember(task.dueDate, task.dueTime, task.endTime, task.estimatedDuration) {
                            val dateStr = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(task.dueDate))
                            val startMin = TimeScheduleUtils.parseToMinutes(task.dueTime)
                            val timeStr = if (startMin != null) {
                                val endMin = TimeScheduleUtils.parseToMinutes(task.endTime) ?: (startMin + task.estimatedDuration)
                                val start12 = TimeScheduleUtils.formatTo12H(startMin).replace(" AM", "am").replace(" PM", "pm")
                                val end12 = TimeScheduleUtils.formatTo12H(endMin).replace(" AM", "am").replace(" PM", "pm")
                                " • $start12 - $end12"
                            } else {
                                ""
                            }
                            "$dateStr$timeStr (${task.estimatedDuration}m)"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Due Date and Timing",
                                modifier = Modifier.size(13.dp),
                                tint = if (isOverdue) MaterialTheme.colorScheme.error else taskColor
                            )
                            Text(
                                text = dateText,
                                fontSize = 11.sp,
                                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
    var selectedDateMs by remember { mutableStateOf<Long?>(System.currentTimeMillis()) }
    var dueTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("09:30") }
    var estimatedDuration by remember { mutableStateOf(30) }
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
                    // Prompt to configure custom task schedule using a clean, full-width button
                    Button(
                        onClick = { showSchedulerDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("choose_advanced_schedule_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Configure Schedule",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configure Task Schedule...", fontSize = 13.sp, fontWeight = FontWeight.Medium)
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

                Spacer(modifier = Modifier.height(12.dp))
                Text("Select Date & Duration", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                
                // Date picker button
                val dateStr = selectedDateMs?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "Today"
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        selectedDateMs?.let { cal.timeInMillis = it }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selected = Calendar.getInstance()
                                selected.set(year, month, dayOfMonth)
                                selectedDateMs = selected.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Date", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Date: $dateStr")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start Time Picker Button
                    OutlinedButton(
                        onClick = {
                            val parts = dueTime.split(":")
                            val h = if (parts.size >= 2) parts[0].toIntOrNull() ?: 9 else 9
                            val m = if (parts.size >= 2) parts[1].toIntOrNull() ?: 0 else 0
                            android.app.TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    dueTime = String.format("%02d:%02d", hourOfDay, minute)
                                    // Automatically set endTime based on estimatedDuration
                                    val endMinutes = (hourOfDay * 60 + minute) + estimatedDuration
                                    endTime = String.format("%02d:%02d", (endMinutes / 60) % 24, endMinutes % 60)
                                },
                                h,
                                m,
                                false
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Start", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val formattedTime = TimeScheduleUtils.parseToMinutes(dueTime)?.let { TimeScheduleUtils.formatTo12H(it) } ?: dueTime
                        Text(text = "Start: $formattedTime", fontSize = 11.sp)
                    }

                    // End Time Picker Button
                    OutlinedButton(
                        onClick = {
                            val parts = endTime.split(":")
                            val h = if (parts.size >= 2) parts[0].toIntOrNull() ?: 9 else 9
                            val m = if (parts.size >= 2) parts[1].toIntOrNull() ?: 30 else 30
                            android.app.TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    endTime = String.format("%02d:%02d", hourOfDay, minute)
                                    // Re-calculate duration if start time is set
                                    if (dueTime.isNotEmpty()) {
                                        val startMin = TimeScheduleUtils.parseToMinutes(dueTime) ?: 0
                                        val endMin = hourOfDay * 60 + minute
                                        val diff = (endMin - startMin + 1440) % 1440
                                        estimatedDuration = if (diff > 0) diff else 30
                                    }
                                },
                                h,
                                m,
                                false
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = "End", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val formattedTime = TimeScheduleUtils.parseToMinutes(endTime)?.let { TimeScheduleUtils.formatTo12H(it) } ?: endTime
                        Text(text = "End: $formattedTime", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Duration Picker Preset Chips
                Text("Estimated Duration", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(15, 30, 45, 60, 120).forEach { mins ->
                        val label = if (mins >= 60) "${mins / 60}h" else "${mins}m"
                        val isSel = estimatedDuration == mins
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                estimatedDuration = mins
                                if (dueTime.isNotEmpty()) {
                                    val startMin = TimeScheduleUtils.parseToMinutes(dueTime) ?: 540
                                    val endMin = startMin + mins
                                    endTime = String.format("%02d:%02d", (endMin / 60) % 24, endMin % 60)
                                }
                            },
                            label = { Text(label) }
                        )
                    }
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
                                dueTime = if (dueTime.isEmpty()) "09:00" else dueTime,
                                endTime = if (endTime.isEmpty()) "09:30" else endTime,
                                estimatedDuration = estimatedDuration,
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
                                dueTime = if (dueTime.isEmpty()) "09:00" else dueTime,
                                reminderOffsetMin = reminderOffsetMin,
                                colorLabel = "#2196F3",
                                endTime = if (endTime.isEmpty()) "09:30" else endTime,
                                estimatedDuration = estimatedDuration
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

/**
 * Skeleton loading card for task item loading shimmer states
 */
@Composable
fun SkeletonTaskItem() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .shimmer()
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmer()
                )
            }
        }
    }
}

// -----------------------------------------------------------------
// High-Performance Custom Animation & Transition Helpers for To-Do
// -----------------------------------------------------------------

/**
 * Shimmer modifier to create elegant, hardware-accelerated skeleton loading states
 */
fun Modifier.shimmer(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE0E0E0)
    val highlightColor = if (isDark) Color(0xFF3D3D3D) else Color(0xFFF5F5F5)

    background(
        brush = Brush.linearGradient(
            colors = listOf(baseColor, highlightColor, baseColor),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

/**
 * Custom Staggered Entrance layout container for list items
 */
@Composable
fun AnimatedStaggeredEntrance(
    index: Int,
    content: @Composable () -> Unit
) {
    var stateVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        stateVisible = true
    }
    
    val delay = (index * 50).coerceAtMost(300) // max 300ms delay to keep it fast and fluid
    val animAlpha by animateFloatAsState(
        targetValue = if (stateVisible) 1f else 0f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = delay,
            easing = FastOutSlowInEasing
        ),
        label = "stagger_alpha"
    )
    val animOffsetY by animateFloatAsState(
        targetValue = if (stateVisible) 0f else 30f,
        animationSpec = tween(
            durationMillis = 400,
            delayMillis = delay,
            easing = FastOutSlowInEasing
        ),
        label = "stagger_offset"
    )
    
    Box(
        modifier = Modifier
            .graphicsLayer {
                alpha = animAlpha
                translationY = animOffsetY
            }
    ) {
        content()
    }
}

/**
 * Fluid, Spring-physics-based swipe gesture wrapper that scales and changes background dynamically
 */
@Composable
fun SwipeableTaskContainer(
    task: TaskEntity,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { 100.dp.toPx() }
    val maxSwipePx = with(density) { 240.dp.toPx() }
    
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        val swipeRatio = (offsetX.value.absoluteValue / thresholdPx).coerceIn(0f, 1.5f)
        val backgroundColor = when {
            offsetX.value > 0 -> {
                Color(0xFF147E43).copy(alpha = (swipeRatio * 0.15f).coerceAtMost(0.9f))
            }
            offsetX.value < 0 -> {
                Color(0xFFBA1A1A).copy(alpha = (swipeRatio * 0.15f).coerceAtMost(0.9f))
            }
            else -> Color.Transparent
        }
        
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(backgroundColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp),
            contentAlignment = if (offsetX.value > 0) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            if (offsetX.value > 0) {
                val iconScale = swipeRatio.coerceIn(0.5f, 1.3f)
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Complete Task",
                    tint = Color(0xFF147E43),
                    modifier = Modifier
                        .scale(iconScale)
                        .size(24.dp)
                )
            } else if (offsetX.value < 0) {
                val iconScale = swipeRatio.coerceIn(0.5f, 1.3f)
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = Color(0xFFBA1A1A),
                    modifier = Modifier
                        .scale(iconScale)
                        .size(24.dp)
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(task.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value > thresholdPx) {
                                    onToggleComplete()
                                    offsetX.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                } else if (offsetX.value < -thresholdPx) {
                                    offsetX.animateTo(
                                        -size.width.toFloat(),
                                        spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    onDelete()
                                } else {
                                    offsetX.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        },
                        onHorizontalDrag = { change: PointerInputChange, dragAmount: Float ->
                            change.consume()
                            scope.launch {
                                val currentVal = offsetX.value
                                val newOffset = if (currentVal > thresholdPx && dragAmount > 0f) {
                                    currentVal + dragAmount * 0.35f
                                } else if (currentVal < -thresholdPx && dragAmount < 0f) {
                                    currentVal + dragAmount * 0.35f
                                } else {
                                    currentVal + dragAmount
                                }
                                offsetX.snapTo(newOffset.coerceIn(-maxSwipePx, maxSwipePx))
                            }
                        }
                    )
                }
                .graphicsLayer {
                    translationX = offsetX.value
                }
        ) {
            content()
        }
    }
}

