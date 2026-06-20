package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // Tasks Queries
    @Query("SELECT * FROM tasks ORDER BY isPinned DESC, createdDate DESC")
    fun getAllTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("UPDATE tasks SET isCompleted = :isCompleted, updatedDate = :updatedDate WHERE id IN (:taskIds)")
    suspend fun updateTasksCompletionStatus(taskIds: List<Long>, isCompleted: Boolean, updatedDate: Long)

    @Query("DELETE FROM tasks WHERE id IN (:taskIds)")
    suspend fun deleteMultipleTasks(taskIds: List<Long>)

    @Query("DELETE FROM tasks WHERE scheduleGroupId = :groupId")
    suspend fun deleteTasksByGroupId(groupId: String)

    @Query("SELECT * FROM tasks WHERE scheduleGroupId = :groupId")
    suspend fun getTasksByGroupId(groupId: String): List<TaskEntity>

    // Categories Queries
    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id AND isSystem = 0")
    suspend fun deleteCategoryById(id: Long)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoriesCount(): Int
}
