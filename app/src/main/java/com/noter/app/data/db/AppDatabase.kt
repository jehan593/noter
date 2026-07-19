package com.noter.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.noter.app.data.db.dao.TaskDao
import com.noter.app.data.db.entity.TaskEntity

@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        const val DATABASE_NAME = "noter.db"
    }
}
