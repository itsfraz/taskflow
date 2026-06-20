package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: TaskViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeState.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val currentScale by viewModel.fontSizeScale.collectAsState()
    val currentLang by viewModel.selectedLanguage.collectAsState()

    var showResetConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings", 
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
            // Section 1: Visual Theme Customization
            Text(
                text = "AESTHETIC THEME", 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary, 
                fontSize = 11.sp,
                letterSpacing = 1.1.sp
            )
            Card(
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Select Theme Mode", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("System", "Light", "Dark").forEach { theme ->
                            val isSelected = currentTheme == theme
                            ElevatedFilterChip(
                                modifier = Modifier.weight(1f),
                                selected = isSelected,
                                onClick = { viewModel.setTheme(theme) },
                                label = { Text(theme, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Section 2: Core Notifications Alerts
            Text(
                text = "ALERTS & REMINDERS", 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary, 
                fontSize = 11.sp,
                letterSpacing = 1.1.sp
            )
            Card(
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Task Reminders", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Receive local notifications when tasks are due", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it, context) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Section 3: Text Scale
            Text(
                text = "ACCESSIBILITY OPTIONS", 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary, 
                fontSize = 11.sp,
                letterSpacing = 1.1.sp
            )
            Card(
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Font Size Multiplier", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.8f to "Small", 1.0f to "Normal", 1.2f to "Large", 1.4f to "Extra").forEach { (scale, name) ->
                            val isSelected = currentScale == scale
                            ElevatedFilterChip(
                                modifier = Modifier.weight(1f),
                                selected = isSelected,
                                onClick = { viewModel.setFontSize(scale) },
                                label = { Text(name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 11.sp) },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Section 4: Local Storage Backup & Clean
            Text(
                text = "DATA BACKUP & CSV", 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary, 
                fontSize = 11.sp,
                letterSpacing = 1.1.sp
            )
            Card(
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val success = viewModel.backupDb()
                            val msg = if (success) "Backup created successfully" else "Failed to create backup"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Backup, "Backup")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup database locally")
                    }

                    OutlinedButton(
                        onClick = {
                            val success = viewModel.restoreDb()
                            val msg = if (success) "Backup restored successfully" else "No backup file found"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Restore, "Restore")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore database")
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CSV OPERATIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.getCsvString { csv ->
                                    Toast.makeText(context, "CSV exported: ${csv.length} chars", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, "Export")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV")
                        }

                        OutlinedButton(
                            onClick = {
                                val dummyCsv = "Title,Description,Category,Priority,DueDate,DueTime,ReminderTime,RepeatOption,ColorLabel,IsCompleted,IsPinned,IsFavorite,IsArchived\n" +
                                        "Work Meeting,Review slides,Work,High,2017004123000,,0,None,#2196F3,false,false,false,false"
                                viewModel.importCsv(dummyCsv) { imported ->
                                    Toast.makeText(context, "Imported $imported items successfully from CSV", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, "Import")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import CSV")
                        }
                    }
                }
            }

            // Section 5: App Information
            Text(
                text = "APP DETAILS", 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary, 
                fontSize = 11.sp,
                letterSpacing = 1.1.sp
            )
            Card(
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Version", fontSize = 13.sp)
                        Text("1.0.0 (Offline-first)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    TextButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, "About")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("About TaskFlow & Licenses")
                    }

                    Button(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DeleteForever, "Reset")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wipe and reset all application data", color = Color.White)
                    }
                }
            }
        }
    }

    // Confirmation delete dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Wipe Application Data?") },
            text = { Text("Are you absolutely sure you want to delete all tasks, categories, and preferences? This will restore files to their blank post-installation state immediately.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showResetConfirm = false
                        Toast.makeText(context, "All data successfully erased.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Erase Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Dismiss")
                }
            }
        )
    }

    // About metadata dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About TaskFlow") },
            text = {
                Column {
                    Text(
                        "TaskFlow is a sophisticated offline-first task organizer developed using complete Jetpack Compose, " +
                        "MVVM architecture, Room Database local cache, DataStore Preferences, and a beautiful custom graphing layer.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Developer: Mohd Faraj", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Build: Production Stable 2026", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Privacy Policy: All application data persists safely on your local device storage. No credentials, metrics, or personal inputs are transmitted over the cloud.", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
