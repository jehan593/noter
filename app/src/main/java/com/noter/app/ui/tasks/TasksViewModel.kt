package com.noter.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noter.app.data.db.entity.TaskEntity
import com.noter.app.data.repository.TasksRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking

class TasksViewModel(private val repository: TasksRepository) : ViewModel() {

    // Completed tasks sink to the bottom so the app list and widget preview agree.
    val tasks: StateFlow<List<TaskEntity>> =
        repository.tasksFlow
            .map { list -> list.sortedBy { it.isDone } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions run on the main thread so each Room write (and its widget refresh) finishes
    // before the user can background the app mid-save.

    fun addTask(text: String) {
        if (text.isNotBlank()) runBlocking { repository.addTask(text.trim()) }
    }

    fun toggleDone(task: TaskEntity) {
        runBlocking { repository.toggleDone(task) }
    }

    fun renameTask(task: TaskEntity, newText: String) {
        if (newText.isNotBlank()) runBlocking { repository.renameTask(task, newText.trim()) }
    }

    fun commitReorder(orderedTasks: List<TaskEntity>) {
        runBlocking { repository.reorder(orderedTasks) }
    }

    fun deleteTask(task: TaskEntity) {
        runBlocking { repository.delete(task) }
    }

    fun markAllDone() {
        runBlocking { repository.markAllDone() }
    }

    fun unmarkAllDone() {
        runBlocking { repository.unmarkAllDone() }
    }

    fun deleteCompleted() {
        runBlocking { repository.deleteCompleted() }
    }

    fun deleteAll() {
        runBlocking { repository.deleteAll() }
    }
}
