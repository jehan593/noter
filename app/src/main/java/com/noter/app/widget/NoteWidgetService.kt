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
 * Position 0 always renders — a dimmed placeholder string when the note is blank, the real note
 * otherwise — rather than count 0 handing off to a separate setEmptyView sibling. A widget region
 * with zero touch-handling of its own (no ListView, no click) turned out to pick up a default
 * tap-to-open fallback on some launchers; keeping the ListView always populated keeps this area
 * always backed by real touch handling, matching the (correctly inert) non-blank case.
 *
 * Filler rows are appended after that first row, sized to just reach (not wildly exceed) the
 * widget's actual current size — see computeFillerRowCount's doc for why that matters.
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
        val estimatedLines = if (note.isBlank()) 1 else note.lines().size
        fillerCount = computeFillerRowCount(context, appWidgetId, contentHeightDp = estimatedLines * NOTE_LINE_HEIGHT_DP)
    }

    override fun getCount(): Int = 1 + fillerCount

    override fun getViewAt(position: Int): RemoteViews {
        if (position > 0) {
            return RemoteViews(context.packageName, R.layout.widget_filler_row)
        }

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
