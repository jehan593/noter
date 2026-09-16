package com.noter.app.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.noter.app.R
import com.noter.app.ui.rememberAppContainer
import com.noter.app.ui.components.ActionIconButton

@Composable
fun NotesScreen() {
    val container = rememberAppContainer()
    val viewModel: NotesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NotesViewModel(container.notesRepository) }
        }
    )
    val noteText by viewModel.noteText.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestViewModel by rememberUpdatedState(viewModel)

    // Save synchronously when the app pauses so the last text always reaches the widget.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                latestViewModel.flushPendingSaveBlocking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Also flush when switching tabs — no lifecycle event fires then.
            latestViewModel.flushPendingSaveBlocking()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActionIconButton(
                onClick = { clipboardManager.setText(AnnotatedString(noteText)) },
                icon = painterResource(R.drawable.ic_copy),
                contentDescription = "Copy note",
                tint = MaterialTheme.colorScheme.primary
            )
            ActionIconButton(
                onClick = { viewModel.onClear() },
                icon = painterResource(R.drawable.ic_delete_all),
                contentDescription = "Clear note",
                tint = MaterialTheme.colorScheme.error
            )
        }
        // Kept in the layout even when empty so the note box below doesn't jump around.
        Text(
            text = statusMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        OutlinedTextField(
            value = noteText,
            onValueChange = { viewModel.onTextChange(it) },
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp),
            placeholder = { Text("Type your note…") },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}