package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val stats = viewModel.getTaskStats()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Productivity Stats", 
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats summary row of cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatSmallCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Active",
                    value = stats.total.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                StatSmallCard(
                    modifier = Modifier.weight(1f),
                    label = "Completed",
                    value = stats.completed.toString(),
                    color = Color(0xFF2E7D32)
                )
                StatSmallCard(
                    modifier = Modifier.weight(1f),
                    label = "Overdue",
                    value = stats.overdue.toString(),
                    color = Color(0xFFC62828)
                )
            }

            // Longest productivity streak banner (Sleek container matching Jordan's style)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CURRENT PRODUCTIVE STREAK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${stats.currentStreak} Days Active!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Beautiful Canvas Pie / Donut Chart Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Task Distribution Analyzer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        DonutChart(
                            completed = stats.completed,
                            pending = stats.pending,
                            overdue = stats.overdue
                        )

                        // Chart legends row
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LegendRow(color = Color(0xFF2E7D32), label = "Completed (${stats.completed})")
                            LegendRow(color = Color(0xFF005FB0), label = "Pending (${stats.pending})")
                            LegendRow(color = Color(0xFFC62828), label = "Overdue (${stats.overdue})")
                        }
                    }
                }
            }

            // Beautiful Custom Bar Chart Card (Completions past 7 days)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Weekly Completion Trend",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    CustomBarChart(data = stats.chartWeeklyCompletions)
                }
            }

            // Gamified Achievement & XP System (World-Class Feature)
            AchievementSection(stats = stats)
        }
    }
}

data class BadgeItem(
    val title: String,
    val description: String,
    val icon: String,
    val requiredTasks: Int = 0,
    val requiredStreak: Int = 0
)

@Composable
fun AchievementSection(stats: com.example.ui.TaskStats) {
    val totalCompleted = stats.completed
    val currentStreak = stats.currentStreak
    val xpPoints = totalCompleted * 15 + currentStreak * 25
    val userLevel = 1 + xpPoints / 100
    val xpForNextLevel = 100 - (xpPoints % 100)
    val progressFraction = (xpPoints % 100).toFloat() / 100f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Level and XP Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Gamified Achievements",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Complete goals to gain XP and Level up!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Lvl $userLevel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$xpPoints XP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                LinearProgressIndicator(
                    progress = progressFraction,
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$xpForNextLevel XP to Lvl ${userLevel + 1}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Milestone Badges",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Badge definitions
            val badges = listOf(
                BadgeItem("Spark Novice", "Complete your first task", "🥉", requiredTasks = 1),
                BadgeItem("Focus Catalyst", "Complete 5 tasks", "🥈", requiredTasks = 5),
                BadgeItem("Flow Champion", "Complete 10 tasks", "🏅", requiredTasks = 10),
                BadgeItem("Zen consistency", "Maintained active 3 day streak", "🔥", requiredStreak = 3),
                BadgeItem("Task Architect", "Complete 20 tasks", "💎", requiredTasks = 20)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                badges.forEach { badge ->
                    val isUnlocked = (totalCompleted >= badge.requiredTasks) && (currentStreak >= badge.requiredStreak)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = badge.icon,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(6.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = badge.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = badge.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2E7D32))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("UNLOCKED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("LOCKED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatSmallCard(
    modifier: Modifier,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(), 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), 
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DonutChart(completed: Int, pending: Int, overdue: Int) {
    val total = completed + pending + overdue
    val completedSweep = if (total > 0) (completed.toFloat() / total) * 360f else 0f
    val pendingSweep = if (total > 0) (pending.toFloat() / total) * 360f else 0f
    val overdueSweep = if (total > 0) (overdue.toFloat() / total) * 360f else 0f

    val animateValue = animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "pie animation"
    ).value

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val strokeWidth = 14.dp.toPx()
            val canvasSize = size.width
            val outerRadius = (canvasSize - strokeWidth)

            // Draw completed slice
            if (completedSweep > 0) {
                drawArc(
                    color = Color(0xFF2E7D32),
                    startAngle = -90f,
                    sweepAngle = completedSweep * animateValue,
                    useCenter = false,
                    size = Size(outerRadius, outerRadius),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    style = Stroke(width = strokeWidth)
                )
            }

            // Draw pending slice
            if (pendingSweep > 0) {
                drawArc(
                    color = Color(0xFF005FB0),
                    startAngle = -90f + completedSweep,
                    sweepAngle = pendingSweep * animateValue,
                    useCenter = false,
                    size = Size(outerRadius, outerRadius),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    style = Stroke(width = strokeWidth)
                )
            }

            // Draw overdue slice
            if (overdueSweep > 0) {
                drawArc(
                    color = Color(0xFFC62828),
                    startAngle = -90f + completedSweep + pendingSweep,
                    sweepAngle = overdueSweep * animateValue,
                    useCenter = false,
                    size = Size(outerRadius, outerRadius),
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
        
        // Mid center label
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$total", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Tasks", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun CustomBarChart(data: List<com.example.ui.ChartData>) {
    if (data.isEmpty()) return
    
    val maxCompletions = remember(data) { data.maxOfOrNull { it.value } ?: 1 }
    val maxVal = if (maxCompletions == 0) 1 else maxCompletions

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { item ->
            val fillPercent = item.value.toFloat() / maxVal
            val animHeight = animateFloatAsState(
                targetValue = fillPercent,
                animationSpec = tween(durationMillis = 800),
                label = "bar heights"
            ).value

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                // Value text indicator above bar
                Text(
                    text = item.value.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Beautiful bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.7f * animHeight + 0.05f) // offset minimum to see some bar
                        .width(22.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Day of the Week text label
                Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
