package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [TaskEntity::class, CategoryEntity::class], version = 3, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_flow_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDefaultCategories(database.taskDao())
                }
            }
        }

        private suspend fun populateDefaultCategories(dao: TaskDao) {
            val defaults = listOf(
                CategoryEntity(name = "Personal", iconName = "person", colorHex = "#4CAF50", isSystem = true),
                CategoryEntity(name = "Work", iconName = "work", colorHex = "#2196F3", isSystem = true),
                CategoryEntity(name = "Study", iconName = "school", colorHex = "#9C27B0", isSystem = true),
                CategoryEntity(name = "Shopping", iconName = "shopping_cart", colorHex = "#FF9800", isSystem = true),
                CategoryEntity(name = "Health", iconName = "favorite", colorHex = "#E91E63", isSystem = true),
                CategoryEntity(name = "Finance", iconName = "account_balance_wallet", colorHex = "#009688", isSystem = true),
                CategoryEntity(name = "Travel", iconName = "flight", colorHex = "#3F51B5", isSystem = true)
            )
            for (category in defaults) {
                dao.insertCategory(category)
            }
        }
    }
}
