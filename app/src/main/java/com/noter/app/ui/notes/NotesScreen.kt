package com.noter.app.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

@Composable
fun NotesScreen() {
    val container = rememberAppContainer()
    val viewModel: NotesViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NotesViewModel(container.notesRepository) }
        }
    )
    val noteText by viewModel.noteText.collectAsState()
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
        }
        OutlinedTextField(
            value = noteText,
            onValueChange = { viewModel.onTextChange(it) },
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp),
            placeholder = { Text("Type your note…") }
        )
    }
}
