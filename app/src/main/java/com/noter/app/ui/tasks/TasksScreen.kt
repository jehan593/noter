package com.noter.app.ui.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.noter.app.ui.rememberAppContainer
import com.noter.app.ui.theme.nord11
import com.noter.app.ui.theme.nord12
import com.noter.app.ui.theme.nord14
import com.noter.app.ui.theme.nord9

@Composable
fun TasksScreen() {
    val container = rememberAppContainer()
    val viewModel: TasksViewModel = viewModel(
        factory = viewModelFactory {
            initializer { TasksViewModel(container.tasksRepository) }
        }
    )
    val tasks by viewModel.tasks.collectAsState()
    var newTaskText by remember { mutableStateOf("") }

    fun submitNewTask() {
        viewModel.addTask(newTaskText)
        newTaskText = ""
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Delete completed / Delete all — the two destructive actions — sit up top, above the
        // add-task box. Check all / Uncheck all move below the add-task box instead, closer to
        // the task list they act on.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TaskActionButton(
                label = "Delete completed",
                tint = nord12,
                onClick = { viewModel.deleteCompleted() }
            )
            TaskActionButton(
                label = "Delete all",
                tint = nord11,
                onClick = { viewModel.deleteAll() }
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = newTaskText,
                onValueChange = { newTaskText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a task…") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitNewTask() })
            )
            IconButton(onClick = { submitNewTask() }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TaskActionButton(
                label = "Check all",
                tint = nord14,
                onClick = { viewModel.markAllDone() }
            )
            TaskActionButton(
                label = "Uncheck all",
                tint = nord9,
                onClick = { viewModel.unmarkAllDone() }
            )
        }
        DraggableTaskList(
            tasks = tasks,
            onToggle = { viewModel.toggleDone(it) },
            onDelete = { viewModel.deleteTask(it) },
            onCommitReorder = { viewModel.commitReorder(it) },
            modifier = Modifier.fillMaxSize().padding(top = 8.dp)
        )
    }
}

@Composable
private fun TaskActionButton(
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = modifier.height(34.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint),
        border = BorderStroke(1.dp, tint)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
