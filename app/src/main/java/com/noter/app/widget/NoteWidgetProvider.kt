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
 * Powers the note widget. Updates are pushed through a broadcast so they get a proper
 * background grace period even when the app was just backgrounded.
 */
class NoteWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NoteWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                // Re-query the list rows without rebuilding and reconnecting the adapter.
                manager.notifyAppWidgetViewDataChanged(ids, R.id.list)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context, appWidgetManager, appWidgetIds)
    }

    // Re-match filler rows whenever the widget is placed or resized.
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
        val views = RemoteViews(context.packageName, R.layout.widget_note)

        val serviceIntent = Intent(context, NoteWidgetService::class.java)
        views.setRemoteAdapter(R.id.list, serviceIntent)

        val launchIntent = buildLaunchIntent(context, MainActivity.TAB_NOTES)
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // The title chip is the single, reliable tap target that opens the app.
        views.setOnClickPendingIntent(R.id.title, pendingIntent)
        return views
    }

    companion object {
        private const val ACTION_REFRESH = "com.noter.app.widget.ACTION_REFRESH_NOTE"
        private const val REQUEST_CODE = 1001

        fun requestUpdate(context: Context) {
            val intent = Intent(context, NoteWidgetProvider::class.java).apply { action = ACTION_REFRESH }
            context.sendBroadcast(intent)
        }
    }
}
