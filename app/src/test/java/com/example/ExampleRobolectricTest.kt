package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.TaskDatabase
import com.example.data.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: TaskDatabase
  private lateinit var context: Context

  @Before
  fun createDb() {
    context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, TaskDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  @Throws(IOException::class)
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("TaskFlow", appName)
  }

  @Test
  fun `add and complete task successfully`() = runBlocking {
    val taskDao = db.taskDao()

    val task = TaskEntity(
      title = "Test Task Title",
      description = "Test Task Description",
      category = "Work",
      priority = "High",
      dueDate = null,
      dueTime = null,
      reminderTime = null,
      repeatOption = "None",
      colorLabel = "#005FB0",
      isCompleted = false
    )

    // Insert task
    val id = taskDao.insertTask(task)
    assertTrue("Task ID should be greater than 0", id > 0)

    // Fetch task from flow
    val allTasks = taskDao.getAllTasksFlow().first()
    assertEquals(1, allTasks.size)
    val fetchedTask = allTasks[0]
    assertEquals("Test Task Title", fetchedTask.title)
    assertEquals("Test Task Description", fetchedTask.description)
    assertEquals(false, fetchedTask.isCompleted)

    // Toggle/complete task
    val completedTask = fetchedTask.copy(isCompleted = true)
    taskDao.updateTask(completedTask)

    // Verify completion status
    val updatedTasks = taskDao.getAllTasksFlow().first()
    assertEquals(1, updatedTasks.size)
    assertTrue("Task should be completed", updatedTasks[0].isCompleted)
  }
}
