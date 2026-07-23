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
 * Classic AppWidgetProvider + RemoteViews rather than Jetpack Glance. Glance's own update path
 * (calling GlanceAppWidget.updateAll from ordinary app code) has no protection against Android's
 * cached-app freezer suspending the update mid-flight right after the app backgrounds. Routing
 * every refresh through a broadcast into this receiver's onReceive/goAsync() gives the update a
 * proper background-execution grace period instead, which is the same pattern battle-tested note
 * apps (e.g. Notally) use for their widgets.
 *
 * Content itself is a ListView backed by NoteWidgetService/NoteWidgetService's RemoteViewsFactory
 * (see that file) — the only way to get real scrolling in a widget.
 */
class NoteWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NoteWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                // Data-only refresh: notifyAppWidgetViewDataChanged asks the already-connected
                // ListView adapter to requery, without tearing down and reconnecting it the way a
                // full updateAppWidget(buildViews()) does via setRemoteAdapter. Reconnecting on
                // every keystroke/mutation left a brief window right after the list changed (e.g.
                // going from empty to populated) where rows were visually present but not yet
                // fully wired for clicks — most noticeable in the filler rows near the bottom.
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
        // The title chip is the ONLY tap-to-open target — deliberately not the ListView content
        // area (no setPendingIntentTemplate/fillInIntent) or root/empty. That ListView-mediated
        // click path was the source of the flaky/inconsistent taps; the content area is read-only
        // (scroll only) now, and the title is the single, reliable, always-clickable affordance.
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
