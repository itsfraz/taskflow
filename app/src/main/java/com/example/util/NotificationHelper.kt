package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val reminderChannel = NotificationChannel(
            "task_reminders",
            "Task Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for Task due reminders and alerts"
        }

        val summaryChannel = NotificationChannel(
            "productivity_summaries",
            "Productivity Summaries",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel for daily productivity stats and overdue tasks"
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(reminderChannel)
        manager.createNotificationChannel(summaryChannel)
    }
}

fun showReminderNotification(context: Context, taskId: Long, title: String, message: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(context, "task_reminders")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    manager.notify(taskId.toInt(), notification)
}

fun showProductivitySummaryNotification(context: Context, title: String, message: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(context, "productivity_summaries")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    manager.notify(9999, notification)
}
