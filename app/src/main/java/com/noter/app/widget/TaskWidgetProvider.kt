package com.noter.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.noter.app.MainActivity
import com.noter.app.R

/**
 * See NoteWidgetProvider's class doc for why this is classic RemoteViews rather than Glance, and
 * why content is a ListView backed by TaskWidgetService's RemoteViewsFactory rather than static
 * rows — that's the only way to get real scrolling through an arbitrarily long task list.
 */
class TaskWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                // Data-only refresh: notifyAppWidgetViewDataChanged asks the already-connected
                // ListView adapter to requery, without tearing down and reconnecting it the way a
                // full updateAppWidget(buildViews()) does via setRemoteAdapter. Reconnecting on
                // every mutation left a brief window right after the list changed (e.g. going
                // from empty to populated) where rows were visually present but not yet fully
                // wired for clicks — most noticeable in the filler rows near the bottom.
                manager.notifyAppWidgetViewDataChanged(ids, R.id.list)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context, appWidgetManager, appWidgetIds)
    }

    // Fires whenever the widget is placed or resized, with the real, system-reported size —
    // re-running the factory here is what keeps the filler-row count matched to the actual
    // current size instead of a one-size-fits-all guess.
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list)
    }

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            manager.updateAppWidget(id, buildViews(context))
        }
        manager.notifyAppWidgetViewDataChanged(ids, R.id.list)
    }

    private fun buildViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_task)

        val serviceIntent = Intent(context, TaskWidgetService::class.java)
        views.setRemoteAdapter(R.id.list, serviceIntent)

        val launchIntent = buildLaunchIntent(context, MainActivity.TAB_TASKS)
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // The title chip is the ONLY tap-to-open target — deliberately not the ListView content
        // area (no setPendingIntentTemplate/fillInIntent) or root/empty. That ListView-mediated
        // click path was the source of the flaky/inconsistent taps; the content area is read-only
        // (scroll only) now, and the title is the single, reliable, always-clickable affordance.
        views.setOnClickPendingIntent(R.id.title, pendingIntent)
        return views
    }

    companion object {
        private const val ACTION_REFRESH = "com.noter.app.widget.ACTION_REFRESH_TASK"
        private const val REQUEST_CODE = 1002

        fun requestUpdate(context: Context) {
            val intent = Intent(context, TaskWidgetProvider::class.java).apply { action = ACTION_REFRESH }
            context.sendBroadcast(intent)
        }
    }
}
