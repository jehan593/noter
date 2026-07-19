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

    // Completed tasks sink to the bottom (stable sort — order within each group still reflects
    // orderIndex, i.e. your drag arrangement) so the in-app list and the widget preview agree.
    val tasks: StateFlow<List<TaskEntity>> =
        repository.tasksFlow
            .map { list -> list.sortedBy { it.isDone } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deliberately blocking (not viewModelScope.launch): these are one-shot user actions, and
    // Android can freeze a backgrounded app's coroutines mid-flight the instant it's minimized.
    // A fire-and-forget launch here could get frozen before the Room write + widget updateAll()
    // finish, so the action would silently never reach the widget. Blocking on the calling
    // (main) thread guarantees the write + widget refresh complete before this function returns
    // — i.e. before the click handler finishes and the user can background the app.

    fun addTask(text: String) {
        if (text.isNotBlank()) runBlocking { repository.addTask(text.trim()) }
    }

    fun toggleDone(task: TaskEntity) {
        runBlocking { repository.toggleDone(task) }
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
