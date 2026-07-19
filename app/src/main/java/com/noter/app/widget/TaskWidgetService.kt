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
        fillerCount = if (tasks.isEmpty()) 0 else {
            computeFillerRowCount(context, appWidgetId, contentHeightDp = tasks.size * TASK_ROW_HEIGHT_DP)
        }
    }

    // Zero real tasks still means zero here — that's what lets setEmptyView's dedicated
    // full-size "No tasks yet" view take over instead of a page of blank filler rows.
    override fun getCount(): Int = if (tasks.isEmpty()) 0 else tasks.size + fillerCount

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= tasks.size) {
            val views = RemoteViews(context.packageName, R.layout.widget_filler_row)
            views.setOnClickFillInIntent(R.id.filler, Intent())
            return views
        }

        val task = tasks[position]
        val views = RemoteViews(context.packageName, R.layout.widget_task_row)
        views.setTextViewText(R.id.check, if (task.isDone) "☑" else "☐")
        views.setTextViewText(R.id.text, task.text)

        val paintFlags = if (task.isDone) Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG else Paint.ANTI_ALIAS_FLAG
        views.setInt(R.id.text, "setPaintFlags", paintFlags)
        views.setTextColor(R.id.text, context.getColor(if (task.isDone) R.color.nord3 else R.color.nord6))
        views.setTextColor(R.id.check, context.getColor(if (task.isDone) R.color.nord3 else R.color.nord8))

        views.setOnClickFillInIntent(R.id.row, Intent())
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 2
    override fun getItemId(position: Int): Long =
        if (position < tasks.size) tasks[position].id else -(position - tasks.size + 1).toLong()
    override fun hasStableIds(): Boolean = true
}
