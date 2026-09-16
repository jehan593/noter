package com.noter.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.noter.app.NoterApplication
import com.noter.app.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Provides the rows shown in the note widget. The whole note is one grow-to-fit list row,
 * which is what lets it scroll inside the widget.
 */
class NoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        return NoteRemoteViewsFactory(this, appWidgetId)
    }
}

/** Approximate line height of the note row's 14sp text. */
private const val NOTE_LINE_HEIGHT_DP = 20

private class NoteRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var note: String = ""
    private var fillerCount: Int = 0

    override fun onCreate() {}
    override fun onDestroy() {}

    override fun onDataSetChanged() {
        val container = (context.applicationContext as NoterApplication).container
        note = runBlocking { container.notesRepository.noteFlow.first() }
        val estimatedLines = if (note.isBlank()) 1 else note.lines().size
        fillerCount = computeFillerRowCount(context, appWidgetId, contentHeightDp = estimatedLines * NOTE_LINE_HEIGHT_DP, overheadDp = 66)
    }

    override fun getCount(): Int = 1 + fillerCount

    override fun getViewAt(position: Int): RemoteViews {
        if (position > 0) {
            return RemoteViews(context.packageName, R.layout.widget_filler_row)
        }

        // Always show one row (a dimmed placeholder when blank) so the area keeps touch handling.
        if (note.isBlank()) {
            val views = RemoteViews(context.packageName, R.layout.widget_placeholder_row)
            views.setTextViewText(R.id.placeholder, context.getString(R.string.widget_no_note))
            return views
        }

        val views = RemoteViews(context.packageName, R.layout.widget_note_row)
        views.setTextViewText(R.id.text, note)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 3
    override fun getItemId(position: Int): Long = if (position == 0) 0 else -position.toLong()
    override fun hasStableIds(): Boolean = true
}
