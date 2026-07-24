package com.noter.app.ui.tasks

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.noter.app.data.db.entity.TaskEntity
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun DraggableTaskList(
    tasks: List<TaskEntity>,
    onToggle: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    onRename: (TaskEntity) -> Unit,
    onCommitReorder: (List<TaskEntity>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Drives the LazyColumn directly so in-progress drag reordering has somewhere to write
    // immediate visual moves to, without fighting the DB-backed Flow re-emitting mid-drag. Only
    // resynced from the persisted list when not reflecting the drag's own write straight back.
    var localList by remember { mutableStateOf(tasks) }
    LaunchedEffect(tasks) { localList = tasks }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localList = localList.toMutableList().apply { add(to.index, removeAt(from.index)) }
    }

    LazyColumn(state = lazyListState, modifier = modifier) {
        items(localList, key = { it.id }) { task ->
            // A completed task is excluded from reordering entirely: `enabled = false` here drops
            // it out of the library's target set (nothing can ever be dropped into its slot — see
            // TasksViewModel's stable isDone sort, which always keeps completed tasks at the
            // bottom), and `enabled = false` on the drag handle below stops it from being picked
            // up in the first place. Together this caps how far an incomplete task can be dragged
            // down at just above the first completed one, without any manual index math.
            ReorderableItem(reorderableState, key = task.id, enabled = !task.isDone) { isDragging ->
                val elevation by animateDpAsState(if (isDragging) 6.dp else 0.dp, label = "dragElevation")
                Surface(tonalElevation = elevation, shadowElevation = elevation) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .then(if (task.isDone) Modifier.alpha(0.3f) else Modifier)
                                .draggableHandle(
                                    enabled = !task.isDone,
                                    onDragStopped = { onCommitReorder(localList) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⠿", style = MaterialTheme.typography.titleLarge)
                        }
                        Checkbox(checked = task.isDone, onCheckedChange = { onToggle(task) })
                        Text(
                            text = task.text,
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier
                                .weight(1f)
                                .alpha(if (task.isDone) 0.5f else 1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = { onRename(task) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Rename task")
                        }
                        IconButton(onClick = { onDelete(task) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete task")
                        }
                    }
                }
            }
        }
    }
}
