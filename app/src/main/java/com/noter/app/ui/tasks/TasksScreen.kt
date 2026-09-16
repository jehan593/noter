package com.noter.app.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.noter.app.R
import com.noter.app.data.db.entity.TaskEntity
import com.noter.app.ui.components.ActionIconButton
import com.noter.app.ui.rememberAppContainer
import com.noter.app.ui.theme.nord7
import com.noter.app.ui.theme.nord12

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
    var renamingTask by remember { mutableStateOf<TaskEntity?>(null) }

    fun submitNewTask() {
        viewModel.addTask(newTaskText)
        newTaskText = ""
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTaskText,
                onValueChange = { newTaskText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a task…") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitNewTask() })
            )
            ActionIconButton(
                onClick = { submitNewTask() },
                icon = painterResource(R.drawable.ic_add),
                contentDescription = "Add task",
                tint = MaterialTheme.colorScheme.primary,
                size = 48.dp
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionIconButton(
                onClick = { viewModel.markAllDone() },
                icon = painterResource(R.drawable.ic_check_all),
                contentDescription = "Check all tasks",
                tint = nord7
            )
            ActionIconButton(
                onClick = { viewModel.unmarkAllDone() },
                icon = painterResource(R.drawable.ic_uncheck_all),
                contentDescription = "Uncheck all tasks",
                tint = MaterialTheme.colorScheme.primary
            )
            ActionIconButton(
                onClick = { viewModel.deleteCompleted() },
                icon = painterResource(R.drawable.ic_delete_completed),
                contentDescription = "Delete completed tasks",
                tint = nord12
            )
            ActionIconButton(
                onClick = { viewModel.deleteAll() },
                icon = painterResource(R.drawable.ic_delete_all),
                contentDescription = "Delete all tasks",
                tint = MaterialTheme.colorScheme.error
            )
        }
        DraggableTaskList(
            tasks = tasks,
            onToggle = { viewModel.toggleDone(it) },
            onDelete = { viewModel.deleteTask(it) },
            onRename = { renamingTask = it },
            onCommitReorder = { viewModel.commitReorder(it) },
            modifier = Modifier.fillMaxSize().padding(top = 12.dp)
        )
    }

    renamingTask?.let { task ->
        RenameTaskDialog(
            initialText = task.text,
            onDismiss = { renamingTask = null },
            onSave = { newText ->
                viewModel.renameTask(task, newText)
                renamingTask = null
            }
        )
    }
}

@Composable
private fun RenameTaskDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    fun submitRename() {
        if (text.isNotBlank()) onSave(text)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename task") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitRename() })
            )
        },
        confirmButton = {
            Button(
                onClick = { submitRename() },
                enabled = text.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
