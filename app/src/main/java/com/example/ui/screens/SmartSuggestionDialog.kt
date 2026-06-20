package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.SmartSuggestion
import com.example.data.SmartSuggestionHelper
import com.example.data.TaskEntity
import com.example.ui.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartSuggestionDialog(
    viewModel: TaskViewModel,
    existingTasks: List<TaskEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var suggestions by remember { mutableStateOf<List<SmartSuggestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Track which suggestions have been added / applied to avoid duplicates and show success state
    val processedSuggestions = remember { mutableStateMapOf<String, String>() } // Map of suggestion title to status ("Added" or "Applied")

    // Automatically trigger analysis on display
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            suggestions = SmartSuggestionHelper.getSmartSuggestions(existingTasks)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to receive AI suggestions"
        } finally {
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("smart_suggestion_dialog")
                .clip(RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header with AI Icon & Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AI Smart Suggestions",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Powered by Gemini 3.5 Flash",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_suggestion_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close description")
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Analyzing existing workload goals...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Formulating related optimal productivity actions...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else if (errorMessage != null) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error icon",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Setup Required",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: "Task analysis failed",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    isLoading = true
                                    errorMessage = null
                                    scope.launch {
                                        try {
                                            suggestions = SmartSuggestionHelper.getSmartSuggestions(existingTasks)
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Failed to receive AI suggestions"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            ) {
                                Text("Retry Analysis")
                            }
                        }
                    } else if (suggestions.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AssignmentLate,
                                contentDescription = "No suggestions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Smart Suggestions Yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Display the scrollable suggested items
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(suggestions) { suggestion ->
                                val status = processedSuggestions[suggestion.title]
                                SuggestionItemCard(
                                    suggestion = suggestion,
                                    status = status,
                                    onAddClicked = {
                                        val parsePriority = when (suggestion.priority.lowercase()) {
                                            "urgent" -> "Urgent"
                                            "high" -> "High"
                                            "medium" -> "Medium"
                                            else -> "Low"
                                        }
                                        val errorMsg = viewModel.insertTaskWithContext(
                                            context = context,
                                            title = suggestion.title,
                                            description = suggestion.description,
                                            category = suggestion.category,
                                            priority = parsePriority,
                                            dueDate = null,
                                            dueTime = null,
                                            reminderOffsetMin = null,
                                            colorLabel = "#005FB0"
                                        )
                                        if (errorMsg == null) {
                                            processedSuggestions[suggestion.title] = "Added"
                                            Toast.makeText(context, "Added smart task successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    onOptimizeClicked = {
                                        // Optimize existing task description
                                        val matchedTask = existingTasks.find { it.title.equals(suggestion.title, ignoreCase = true) }
                                        if (matchedTask != null) {
                                            val updatedTask = matchedTask.copy(
                                                description = suggestion.description,
                                                updatedDate = System.currentTimeMillis()
                                            )
                                            viewModel.updateTaskWithContext(context, updatedTask)
                                            processedSuggestions[suggestion.title] = "Applied"
                                            Toast.makeText(context, "Optimized task details successfully!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Fallback: Add as new if task was deleted/not found
                                            viewModel.insertTaskWithContext(
                                                context = context,
                                                title = suggestion.title,
                                                description = "Optimized Subtasks:\n" + suggestion.description,
                                                category = suggestion.category,
                                                priority = suggestion.priority,
                                                dueDate = null,
                                                dueTime = null,
                                                reminderOffsetMin = null,
                                                colorLabel = "#005FB0"
                                            )
                                            processedSuggestions[suggestion.title] = "Added"
                                            Toast.makeText(context, "Matched task not found. Added as new instead!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mandatory Security & Prototype Notification warning banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Warning icon",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Security & Prototype Alert",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionItemCard(
    suggestion: SmartSuggestion,
    status: String?, // "Added", "Applied", or null
    onAddClicked: () -> Unit,
    onOptimizeClicked: () -> Unit
) {
    val isNewType = suggestion.type == "new"
    val containerBorderColor = if (isNewType) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, containerBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Suggestion Badge (Type & Priority Chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isNewType) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isNewType) "💡 NEW ACTION" else "⚡ OPTIMIZATION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNewType) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = suggestion.category.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Priority Indicator
                val priorityColor = when (suggestion.priority.lowercase()) {
                    "urgent" -> Color(0xFFC62828)
                    "high" -> Color(0xFFD66000)
                    "medium" -> Color(0xFF005FB0)
                    else -> Color(0xFF2E7D32)
                }
                Text(
                    text = "${suggestion.priority} Prio",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = priorityColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Suggestion Body
            Text(
                text = suggestion.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = suggestion.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (status != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success tick",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (status == "Added") "Smart task added!" else "Workplan applied!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                } else {
                    if (isNewType) {
                        Button(
                            onClick = onAddClicked,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = "Add suggested icon", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add suggested", fontSize = 12.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = onOptimizeClicked,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FlashOn, contentDescription = "Optimize icon", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Optimize task", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
