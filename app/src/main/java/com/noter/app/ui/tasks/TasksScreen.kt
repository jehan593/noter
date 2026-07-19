package com.noter.app.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.noter.app.R
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
        Row(modifier = Modifier.fillMaxWidth()) {
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
        // 2x2 grid rather than a single row — four compact buttons don't comfortably fit one row
        // on a phone-width screen without either truncating labels or scrolling sideways.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskActionButton(
                label = "Mark all",
                icon = Icons.Filled.Done,
                tint = nord14,
                onClick = { viewModel.markAllDone() },
                modifier = Modifier.weight(1f)
            )
            TaskActionButton(
                label = "Unmark all",
                icon = Icons.Filled.Clear,
                tint = nord9,
                onClick = { viewModel.unmarkAllDone() },
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskActionButton(
                label = "Clear done",
                painter = painterResource(R.drawable.ic_delete_done),
                tint = nord12,
                onClick = { viewModel.deleteCompleted() },
                modifier = Modifier.weight(1f)
            )
            TaskActionButton(
                label = "Clear all",
                icon = Icons.Filled.Delete,
                tint = nord11,
                onClick = { viewModel.deleteAll() },
                modifier = Modifier.weight(1f)
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
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    painter: Painter? = null
) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
        modifier = modifier.height(34.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = tint)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        } else if (painter != null) {
            Icon(painter = painter, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Text(label, modifier = Modifier.padding(start = 4.dp))
    }
}
