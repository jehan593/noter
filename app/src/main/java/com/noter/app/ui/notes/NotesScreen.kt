package com.noter.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.noter.app.R
import com.noter.app.ui.rememberAppContainer
import com.noter.app.ui.components.ActionIconButton
import kotlin.math.ceil

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
        NoteEditor(
            text = noteText,
            onTextChange = viewModel::onTextChange,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp)
        )
    }
}

@Composable
private fun NoteEditor(text: String, onTextChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var editorValue by remember { mutableStateOf(TextFieldValue(text)) }
    val value = editorValue.copy(text = text)
    val scrollState = rememberScrollState()
    val editorPaddingPx = with(LocalDensity.current) { 16.dp.toPx() }
    var editorFocused by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val shape = RoundedCornerShape(12.dp)

    LaunchedEffect(text) {
        if (editorValue.text != text) {
            editorValue = editorValue.copy(text = text, composition = null)
        }
    }

    LaunchedEffect(scrollState.viewportSize, value.selection, textLayoutResult, editorFocused) {
        val layout = textLayoutResult ?: return@LaunchedEffect
        val viewportHeight = scrollState.viewportSize
        if (!editorFocused || viewportHeight <= 0) return@LaunchedEffect
        val cursor = layout.getCursorRect(value.selection.end.coerceIn(0, layout.layoutInput.text.length))
        val visibleTop = scrollState.value.toFloat()
        val cursorTop = cursor.top + editorPaddingPx
        val cursorBottom = cursor.bottom + editorPaddingPx
        val target = when {
            cursorBottom + editorPaddingPx > visibleTop + viewportHeight ->
                ceil(cursorBottom + editorPaddingPx - viewportHeight).toInt()
            cursorTop - editorPaddingPx < visibleTop ->
                (cursorTop - editorPaddingPx).toInt()
            else -> return@LaunchedEffect
        }
        scrollState.scrollTo(target.coerceIn(0, scrollState.maxValue))
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (editorFocused) 2.dp else 1.dp,
                color = if (editorFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = shape
            )
    ) {
        val viewportHeight = maxHeight
        Box(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            BasicTextField(
                value = value,
                onValueChange = {
                    editorValue = it
                    if (it.text != text) onTextChange(it.text)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight)
                    .padding(16.dp)
                    .onFocusChanged { editorFocused = it.isFocused },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                onTextLayout = { textLayoutResult = it },
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text("Type your note…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
