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

class NoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        return NoteRemoteViewsFactory(this, appWidgetId)
    }
}

/** Approximate rendered height of one line of widget_note_row.xml's 14sp text. */
private const val NOTE_LINE_HEIGHT_DP = 20

/**
 * A single-row ListView is a deliberate trick: a plain TextView in a widget cannot scroll (Android
 * only gives collection views — ListView/GridView/StackView — real scroll handling), but wrapping
 * the whole note in exactly one ListView row lets that row grow as tall as the text needs while
 * the ListView itself scrolls to reveal all of it. Same pattern Notally uses for its note widget.
 *
 * count is 0 when the note is blank rather than 1 row showing a placeholder string — that lets
 * setEmptyView's real sibling view take over (see widget_note.xml's "empty" TextView), which is
 * clickable across the widget's full current size.
 *
 * When the note isn't blank, filler rows are appended after the real content row, sized to just
 * reach (not wildly exceed) the widget's actual current size — see computeFillerRowCount's doc for
 * why that matters.
 */
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
        fillerCount = if (note.isBlank()) 0 else {
            val estimatedLines = note.lines().size
            computeFillerRowCount(context, appWidgetId, contentHeightDp = estimatedLines * NOTE_LINE_HEIGHT_DP)
        }
    }

    override fun getCount(): Int = if (note.isBlank()) 0 else 1 + fillerCount

    override fun getViewAt(position: Int): RemoteViews {
        if (position > 0) {
            val views = RemoteViews(context.packageName, R.layout.widget_filler_row)
            views.setOnClickFillInIntent(R.id.filler, Intent())
            return views
        }

        val views = RemoteViews(context.packageName, R.layout.widget_note_row)
        views.setTextViewText(R.id.text, note)
        views.setOnClickFillInIntent(R.id.text, Intent())
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 2
    override fun getItemId(position: Int): Long = if (position == 0) 0 else -position.toLong()
    override fun hasStableIds(): Boolean = true
}
