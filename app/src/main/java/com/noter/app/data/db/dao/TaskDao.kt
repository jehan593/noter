package com.noter.app.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.noter.app.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM tasks")
    suspend fun maxOrderIndex(): Int

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Transaction
    @Update
    suspend fun updateAll(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET isDone = 1")
    suspend fun markAllDone()

    @Query("UPDATE tasks SET isDone = 0")
    suspend fun unmarkAllDone()

    @Query("DELETE FROM tasks WHERE isDone = 1")
    suspend fun deleteCompleted()

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
