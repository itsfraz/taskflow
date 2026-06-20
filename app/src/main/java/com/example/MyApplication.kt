package com.example

import android.app.Application
import com.example.data.TaskDatabase
import com.example.data.TaskPreferences
import com.example.data.TaskRepository
import com.example.util.createNotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MyApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { TaskDatabase.getDatabase(this, applicationScope) }
    val preferences by lazy { TaskPreferences(this) }
    val repository by lazy { TaskRepository(this, database.taskDao(), preferences) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
    }
}
