package com.example.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MyApplication
import com.example.util.showReminderNotification
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("task_id", -1)
        if (taskId == -1L) return Result.failure()

        val app = applicationContext as MyApplication
        val task = app.repository.getTaskById(taskId) ?: return Result.success()

        if (!task.isCompleted && !task.isArchived) {
            val title = "Task Due Soon: ${task.title}"
            val message = task.description.ifEmpty { "Do not forget about this task!" }
            showReminderNotification(applicationContext, taskId, title, message)
        }

        return Result.success()
    }

    companion object {
        fun scheduleReminder(context: Context, taskId: Long, triggerTimeMs: Long) {
            val delayMs = triggerTimeMs - System.currentTimeMillis()
            if (delayMs <= 0) return

            val data = Data.Builder()
                .putLong("task_id", taskId)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(data)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .addTag("task_reminder_$taskId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "task_reminder_$taskId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun cancelReminder(context: Context, taskId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("task_reminder_$taskId")
        }
    }
}
