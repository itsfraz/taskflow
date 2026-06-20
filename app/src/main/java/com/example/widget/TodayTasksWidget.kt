package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.TaskDatabase
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodayTasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Query tasks directly from database using first() to get immediate list snapshot
        val database = TaskDatabase.getDatabase(context, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO))
        val allTasks = try {
            database.taskDao().getAllTasksFlow().first()
        } catch (e: Exception) {
            emptyList()
        }

        // Filter today's tasks which are incomplete
        val todayStartEpoch = getStartOfTodayMs()
        val todayEndEpoch = todayStartEpoch + 24 * 60 * 60 * 1000L

        val todayTasks = allTasks.filter { task ->
            !task.isCompleted && (task.dueDate == null || task.dueDate in todayStartEpoch..todayEndEpoch)
        }.take(3) // displays up to 3 for concise fit

        provideContent {
            WidgetContent(context, todayTasks)
        }
    }

    @Composable
    private fun WidgetContent(context: Context, todayTasks: List<com.example.data.TaskEntity>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(android.graphics.Color.parseColor("#0F172A"))) // Obsidian primary background
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = GlanceModifier.fillMaxWidth()
            ) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = "Today's Agenda",
                        style = TextStyle(
                            color = ColorProvider(android.graphics.Color.WHITE),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    val currentDateStr = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())
                    Text(
                        text = currentDateStr,
                        style = TextStyle(
                            color = ColorProvider(android.graphics.Color.parseColor("#94A3B8")),
                            fontSize = 10.sp
                        )
                    )
                }

                // Add button
                Button(
                    text = "+ Quick Add",
                    onClick = actionStartActivity<MainActivity>()
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            if (todayTasks.isEmpty()) {
                // Empty state centered
                Row(
                    modifier = GlanceModifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎉 All done for today!",
                        style = TextStyle(
                            color = ColorProvider(android.graphics.Color.parseColor("#38BDF8")),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            } else {
                // Task render list
                todayTasks.forEach { task ->
                    val bulletDot = when (task.priority) {
                        "Urgent" -> "🔴"
                        "High" -> "🟠"
                        "Medium" -> "🔵"
                        else -> "🟢"
                    }

                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(ColorProvider(android.graphics.Color.parseColor("#1E293B")))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$bulletDot ",
                            style = TextStyle(fontSize = 12.sp)
                        )
                        Text(
                            text = task.title,
                            style = TextStyle(
                                color = ColorProvider(android.graphics.Color.WHITE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
            }
        }
    }

    private fun getStartOfTodayMs(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
