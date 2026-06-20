package com.example.ui.screens

import android.os.CountDownTimer
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(
    onNavigateBack: () -> Unit
) {
    // Mode: "pomodoro" or "stopwatch"
    var selectedMode by remember { mutableStateOf("pomodoro") }

    // Pomodoro timers states
    var workDurationMinutes by remember { mutableIntStateOf(25) }
    var breakDurationMinutes by remember { mutableIntStateOf(5) }
    var isWorkingSession by remember { mutableStateOf(true) } // true for Work, false for Break

    var pomodoroTimeLeftMs by remember { mutableLongStateOf(25 * 60 * 1000L) }
    var isPomodoroRunning by remember { mutableStateOf(false) }

    // Stopwatch states
    var stopwatchTimeMs by remember { mutableLongStateOf(0L) }
    var isStopwatchRunning by remember { mutableStateOf(false) }

    // Ambient sound selector simulation
    var activeAmbientSound by remember { mutableStateOf("None") }

    // Sync isWorkingSession changes to duration
    LaunchedEffect(isWorkingSession, workDurationMinutes, breakDurationMinutes) {
        if (!isPomodoroRunning) {
            pomodoroTimeLeftMs = if (isWorkingSession) {
                workDurationMinutes * 60 * 1000L
            } else {
                breakDurationMinutes * 60 * 1000L
            }
        }
    }

    // Pomodoro Timer background ticker
    LaunchedEffect(isPomodoroRunning) {
        while (isPomodoroRunning && pomodoroTimeLeftMs > 0) {
            delay(1000L)
            pomodoroTimeLeftMs = (pomodoroTimeLeftMs - 1000L).coerceAtLeast(0L)
            if (pomodoroTimeLeftMs == 0L) {
                isPomodoroRunning = false
                // Swap session with standard sound alerts or auto success notifications
                isWorkingSession = !isWorkingSession
                pomodoroTimeLeftMs = if (isWorkingSession) {
                    workDurationMinutes * 60 * 1000L
                } else {
                    breakDurationMinutes * 60 * 1000L
                }
            }
        }
    }

    // Stopwatch background ticker
    LaunchedEffect(isStopwatchRunning) {
        while (isStopwatchRunning) {
            delay(100L)
            stopwatchTimeMs += 100L
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Workspace Focus",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Screen explanation banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Focus Flow Sandbox",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Boost deep work and beat procrastination cycles.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tab Switchers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { selectedMode = "pomodoro" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMode == "pomodoro") MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (selectedMode == "pomodoro") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = "Pomodoro mode icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pomodoro", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { selectedMode = "stopwatch" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMode == "stopwatch") MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (selectedMode == "stopwatch") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Stopwatch mode icon", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stopwatch", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Main Display Screen with transition
            AnimatedContent(
                targetState = selectedMode,
                transitionSpec = {
                    fadeIn() + slideInVertically() with fadeOut() + slideOutVertically()
                },
                label = "FocusModeContent"
            ) { state ->
                if (state == "pomodoro") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Current status info
                        Text(
                            text = if (isWorkingSession) "🔥 DEEP WORK SESSION" else "☕ BREAK BREAK SESSION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isWorkingSession) Color(0xFFC62828) else Color(0xFF2E7D32),
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large beautiful clock display
                        val minutes = (pomodoroTimeLeftMs / 1000) / 60
                        val seconds = (pomodoroTimeLeftMs / 1000) % 60
                        val displayStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

                        Text(
                            text = displayStr,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Custom percentage bar
                        val totalOriginalDuration = if (isWorkingSession) workDurationMinutes else breakDurationMinutes
                        val totalOriginalMs = totalOriginalDuration * 60 * 1000L
                        val fractionFraction = if (totalOriginalMs > 0) {
                            pomodoroTimeLeftMs.toFloat() / totalOriginalMs.toFloat()
                        } else 1.0f

                        LinearProgressIndicator(
                            progress = fractionFraction,
                            color = if (isWorkingSession) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32),
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Play/Pause & Reset Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Reset button
                            FilledIconButton(
                                onClick = {
                                    isPomodoroRunning = false
                                    pomodoroTimeLeftMs = if (isWorkingSession) {
                                        workDurationMinutes * 60 * 1000L
                                    } else {
                                        breakDurationMinutes * 60 * 1000L
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(52.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset Pomodoro")
                            }

                            // Play Pause button
                            FilledIconButton(
                                onClick = { isPomodoroRunning = !isPomodoroRunning },
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp).testTag("pomodoro_toggle_btn"),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (isPomodoroRunning) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPomodoroRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause Button",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Skip session key
                            FilledIconButton(
                                onClick = {
                                    isPomodoroRunning = false
                                    isWorkingSession = !isWorkingSession
                                    pomodoroTimeLeftMs = if (isWorkingSession) {
                                        workDurationMinutes * 60 * 1000L
                                    } else {
                                        breakDurationMinutes * 60 * 1000L
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(52.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Skip Session")
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Duration Sliders Configuration card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Customize Durations (Minutes)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Work slider
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Work: ${workDurationMinutes}m", fontSize = 13.sp)
                                    Slider(
                                        value = workDurationMinutes.toFloat(),
                                        onValueChange = { workDurationMinutes = it.toInt() },
                                        valueRange = 5f..60f,
                                        steps = 11,
                                        modifier = Modifier.fillMaxWidth(0.72f)
                                    )
                                }

                                // Break slider
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Break: ${breakDurationMinutes}m", fontSize = 13.sp)
                                    Slider(
                                        value = breakDurationMinutes.toFloat(),
                                        onValueChange = { breakDurationMinutes = it.toInt() },
                                        valueRange = 1f..30f,
                                        steps = 29,
                                        modifier = Modifier.fillMaxWidth(0.72f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Stopwatch Layout Mode
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "⏱️ CHRONOMETER TRACKER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        val minutes = (stopwatchTimeMs / 1000) / 60
                        val seconds = (stopwatchTimeMs / 1000) % 60
                        val millis = (stopwatchTimeMs % 1000) / 100
                        val displayStr = String.format(Locale.getDefault(), "%02d:%02d.%01d", minutes, seconds, millis)

                        Text(
                            text = displayStr,
                            fontSize = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Reset
                            FilledIconButton(
                                onClick = {
                                    isStopwatchRunning = false
                                    stopwatchTimeMs = 0L
                                },
                                shape = CircleShape,
                                modifier = Modifier.size(52.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reset Stopwatch")
                            }

                            // Start / Stop
                            FilledIconButton(
                                onClick = { isStopwatchRunning = !isStopwatchRunning },
                                shape = CircleShape,
                                modifier = Modifier.size(72.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (isStopwatchRunning) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isStopwatchRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause Button",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Premium Binaural Sound simulation
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = "Sound icon", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Ambient Soundscapes (Binaural Loops)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("None", "Rainforest", "Ocean Waves", "White Noise").forEach { sound ->
                            val isSelected = activeAmbientSound == sound
                            val chipColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(chipColor)
                                    .clickable { activeAmbientSound = sound }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(sound, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
