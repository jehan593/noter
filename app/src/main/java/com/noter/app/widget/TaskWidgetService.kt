package com.noter.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.noter.app.NoterApplication
import com.noter.app.R
import com.noter.app.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TaskWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        return TaskRemoteViewsFactory(this, appWidgetId)
    }
}

/** Approximate rendered height of one widget_task_row.xml (14sp text + 1dp vertical padding each side). */
private const val TASK_ROW_HEIGHT_DP = 28

private class TaskRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var tasks: List<TaskEntity> = emptyList()
    private var fillerCount: Int = 0

    override fun onCreate() {}
    override fun onDestroy() {}

    override fun onDataSetChanged() {
        val container = (context.applicationContext as NoterApplication).container
        val all = runBlocking { container.tasksRepository.tasksFlow.first() }
        // Completed tasks sink to the bottom of the widget preview only — the persisted order
        // used for in-app dragging is untouched, this is purely how the read-only widget lays
        // them out.
        tasks = all.sortedBy { it.isDone }
        val contentRows = maxOf(tasks.size, 1)
        fillerCount = computeFillerRowCount(context, appWidgetId, contentHeightDp = contentRows * TASK_ROW_HEIGHT_DP)
    }

    // Real task count, or 1 for the dimmed placeholder row when empty — rather than 0 handing off
    // to a separate setEmptyView sibling. A widget region with zero touch-handling of its own (no
    // ListView, no click) turned out to pick up a default tap-to-open fallback on some launchers;
    // keeping the ListView always populated keeps this area always backed by real touch handling.
    override fun getCount(): Int = maxOf(tasks.size, 1) + fillerCount

    override fun getViewAt(position: Int): RemoteViews {
        if (tasks.isEmpty() && position == 0) {
            val views = RemoteViews(context.packageName, R.layout.widget_placeholder_row)
            views.setTextViewText(R.id.placeholder, context.getString(R.string.widget_no_tasks))
            return views
        }

        if (position >= tasks.size) {
            return RemoteViews(context.packageName, R.layout.widget_filler_row)
        }

        val task = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_row)
        views.setTextViewText(R.id.check, if (task.isDone) "☑" else "☐")
        views.setTextViewText(R.id.text, task.text)

        val paintFlags = if (task.isDone) Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG else Paint.ANTI_ALIAS_FLAG
        views.setInt(R.id.text, "setPaintFlags", paintFlags)
        views.setTextColor(R.id.text, context.getColor(if (task.isDone) R.color.nord3 else R.color.nord6))
        views.setTextColor(R.id.check, context.getColor(if (task.isDone) R.color.nord3 else R.color.nord8))

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 3
    override fun getItemId(position: Int): Long =
        if (position < tasks.size) tasks[position].id else -(position - tasks.size + 1).toLong()
    override fun hasStableIds(): Boolean = true
}
