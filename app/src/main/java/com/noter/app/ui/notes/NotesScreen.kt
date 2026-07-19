package com.noter.app.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.noter.app.R
import com.noter.app.ui.rememberAppContainer

@Composable
fun NotesScreen() {
    val container = rememberAppContainer()
    val viewModel: NotesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NotesViewModel(container.notesRepository) }
        }
    )
    val noteText by viewModel.noteText.collectAsState()
    val notesnookSettings by viewModel.notesnookSettings.collectAsState()
    val isSettingsDialogOpen by viewModel.isSettingsDialogOpen.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestViewModel by rememberUpdatedState(viewModel)

    // ON_PAUSE (not ON_STOP) is the latest point guaranteed to run before the OS can freeze this
    // process's coroutines mid-background — flushing here synchronously is what makes "type, then
    // immediately minimize" reliably reach the widget instead of racing a frozen coroutine.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                latestViewModel.flushPendingSaveBlocking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Switching to the Tasks tab unmounts this composable without any lifecycle event
            // firing (the Activity itself never leaves ON_RESUME) — flush here too.
            latestViewModel.flushPendingSaveBlocking()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { clipboardManager.setText(AnnotatedString(noteText)) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text("Copy", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(
                onClick = { viewModel.onClear() },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Clear", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(
                onClick = { viewModel.sendToNotesnook() },
                enabled = !isSending,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                // Fixed width sized for the longer "Sending…" label so the button — and anything
                // laid out after it — doesn't shift when the label swaps between the two states.
                modifier = Modifier.height(34.dp).width(120.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    if (isSending) "Sending…" else "Send",
                    modifier = Modifier.padding(start = 6.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }
            // Weighted spacer (rather than relying on the preceding buttons' widths staying
            // constant) absorbs any width change so the settings icon stays pinned to the row's
            // trailing edge no matter what the buttons before it do.
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = { viewModel.openSettingsDialog() },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Notesnook settings", modifier = Modifier.size(18.dp))
            }
        }
        // Always rendered (even when empty) so this line's height is reserved permanently —
        // otherwise the note body below shifts up/down every time a status message appears or
        // clears.
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
            placeholder = { Text("Type your note…") }
        )
    }

    if (isSettingsDialogOpen) {
        NotesnookSettingsDialog(
            initialApiKey = notesnookSettings.apiKey.orEmpty(),
            initialTagId = notesnookSettings.tagId.orEmpty(),
            onDismiss = { viewModel.dismissSettingsDialog() },
            onSave = { apiKey, tagId -> viewModel.saveNotesnookSettings(apiKey, tagId) }
        )
    }
}

@Composable
private fun NotesnookSettingsDialog(
    initialApiKey: String,
    initialTagId: String,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, tagId: String) -> Unit
) {
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var tagId by remember { mutableStateOf(initialTagId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notesnook") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "From Notesnook: Settings > Inbox > Create Key.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Inbox API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagId,
                    onValueChange = { tagId = it },
                    label = { Text("Tag ID (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Right-click a tag in Notesnook and choose Copy ID. Sent notes are titled with " +
                        "the current date and time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(apiKey, tagId) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
