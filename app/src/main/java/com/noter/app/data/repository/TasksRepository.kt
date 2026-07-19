package com.noter.app.data.repository

import android.content.Context
import com.noter.app.data.db.dao.TaskDao
import com.noter.app.data.db.entity.TaskEntity
import com.noter.app.widget.TaskWidgetProvider
import kotlinx.coroutines.flow.Flow

class TasksRepository(
    private val dao: TaskDao,
    private val context: Context
) {
    val tasksFlow: Flow<List<TaskEntity>> = dao.observeAll()

    suspend fun addTask(text: String) {
        val next = dao.maxOrderIndex() + 1
        dao.insert(TaskEntity(text = text, orderIndex = next))
        TaskWidgetProvider.requestUpdate(context)
    }

    suspend fun toggleDone(task: TaskEntity) {
        dao.update(task.copy(isDone = !task.isDone))
        TaskWidgetProvider.requestUpdate(context)
    }

    suspend fun reorder(orderedTasks: List<TaskEntity>) {
        dao.updateAll(orderedTasks.mapIndexed { index, task -> task.copy(orderIndex = index) })
        TaskWidgetProvider.requestUpdate(context)
    }

    suspend fun delete(task: TaskEntity) {
        dao.delete(task)
        TaskWidgetProvider.requestUpdate(context)
    }

    suspend fun markAllDone() {
        dao.markAllDone()
        TaskWidgetProvider.requestUpdate(context)
    }

    suspend fun unmarkAllDone() {
        dao.unmarkAllDone()
        TaskWidgetProvider.requestUpdate(context)
    }

    suspend fun deleteCompleted() {
        dao.deleteCompleted()
        TaskWidgetProvider.requestUpdate(context)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
        TaskWidgetProvider.requestUpdate(context)
    }
}
