package com.noter.app.ui.tasks

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.noter.app.data.db.entity.TaskEntity
import kotlin.math.roundToInt

/** Only populated while a drag is in progress; the list falls back to the live [tasks] otherwise. */
private data class DragState(
    val taskId: Long,
    val currentIndex: Int,
    val offsetY: Float,
    val workingList: List<TaskEntity>
)

@Composable
fun DraggableTaskList(
    tasks: List<TaskEntity>,
    onToggle: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onCommitReorder: (List<TaskEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }
    val haptics = LocalHapticFeedback.current

    // The live, Room-backed list is the source of truth whenever nothing is being dragged right
    // now — only a drag in progress temporarily overrides it with its own working copy.
    val displayList = dragState?.workingList ?: tasks

    LazyColumn(modifier = modifier) {
        itemsIndexed(displayList, key = { _, task -> task.id }) { index, task ->
            val currentIndex by rememberUpdatedState(index)
            val isDragging = dragState?.taskId == task.id

            Surface(
                tonalElevation = if (isDragging) 6.dp else 0.dp,
                shadowElevation = if (isDragging) 6.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 1f else 0f)
                    // Only non-dragged rows animate their position — the dragged row is being
                    // moved by the user's finger in real time via the manual offset below, and
                    // letting animateItem() also chase that would fight the touch and feel laggy.
                    // This is what makes toggling "done" (which reorders via the isDone sort)
                    // glide the row to the bottom instead of popping there instantly.
                    .then(if (isDragging) Modifier else Modifier.animateItem())
                    .graphicsLayer {
                        translationY = if (isDragging) dragState?.offsetY ?: 0f else 0f
                    }
                    .onGloballyPositioned {
                        if (rowHeightPx == 0f) rowHeightPx = it.size.height.toFloat()
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .pointerInput(task.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        dragState = DragState(
                                            taskId = task.id,
                                            currentIndex = currentIndex,
                                            offsetY = 0f,
                                            workingList = displayList
                                        )
                                    },
                                    onDragEnd = {
                                        dragState?.let { onCommitReorder(it.workingList) }
                                        dragState = null
                                    },
                                    onDragCancel = { dragState = null },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val state = dragState ?: return@detectDragGesturesAfterLongPress
                                        if (rowHeightPx <= 0f) return@detectDragGesturesAfterLongPress
                                        var newOffset = state.offsetY + dragAmount.y
                                        var newIndex = state.currentIndex
                                        var newList = state.workingList
                                        val shift = (newOffset / rowHeightPx).roundToInt()
                                        if (shift != 0) {
                                            val to = (state.currentIndex + shift).coerceIn(0, state.workingList.lastIndex)
                                            if (to != state.currentIndex) {
                                                newList = state.workingList.toMutableList().apply {
                                                    add(to, removeAt(state.currentIndex))
                                                }
                                                newIndex = to
                                                newOffset -= shift * rowHeightPx
                                            }
                                        }
                                        dragState = state.copy(currentIndex = newIndex, offsetY = newOffset, workingList = newList)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⠿", style = MaterialTheme.typography.titleLarge)
                    }
                    Checkbox(checked = task.isDone, onCheckedChange = { onToggle(task) })
                    Text(
                        text = task.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier
                            .weight(1f)
                            .alpha(if (task.isDone) 0.5f else 1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(onClick = { onDelete(task) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete task")
                    }
                }
            }
        }
    }
}
