package com.noter.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context

// Last successfully-read widget height, per widget id — a fallback for when a refresh's options
// read momentarily comes back empty (see computeFillerRowCount) so that case reuses the real
// last-known size instead of always dropping to the small hardcoded guess.
private val lastKnownHeightDp = mutableMapOf<Int, Int>()

/**
 * How many blank filler rows (see widget_filler_row.xml) a ListView-backed widget needs so its
 * total content height reaches — but doesn't wildly exceed — the widget's actual current size.
 * Too few and there's dead, unclickable space below short content; too many and the list becomes
 * scrollable well past its real content for no reason (a swipe that should hand off to the home
 * screen instead scrolls into blank space).
 *
 * [contentHeightDp] is the caller's best estimate of how tall the real (non-filler) rows already
 * are. [overheadDp] accounts for the title row and container padding that isn't part of the list.
 *
 * If the size genuinely has never been read for this widget (very first frame before the widget
 * host has reported anything), this deliberately falls back to a SMALL number rather than a
 * generous one — under-filling only brings back a narrow slice of the original dead-space issue in
 * a rare edge case, while over-filling directly reintroduces unwanted scroll, which is the worse
 * regression. But if a size was read successfully before and this particular refresh's read just
 * came back empty (the options bundle not always being populated on every call is a real launcher
 * quirk), reuse that last-known size instead of the small guess — otherwise every such refresh
 * under-fills a widget we already know is bigger than 2 filler rows, leaving real dead space at
 * the bottom that taps land on and silently do nothing.
 */
fun computeFillerRowCount(
    context: Context,
    appWidgetId: Int,
    contentHeightDp: Int,
    fillerRowHeightDp: Int = 40,
    overheadDp: Int = 40,
    maxFillerRows: Int = 10,
    fallbackFillerRows: Int = 2
): Int {
    val heightDp = readHeightDp(context, appWidgetId)?.also { lastKnownHeightDp[appWidgetId] = it }
        ?: lastKnownHeightDp[appWidgetId]
        ?: return fallbackFillerRows

    val availableForListDp = heightDp - overheadDp
    val remainingDp = availableForListDp - contentHeightDp
    if (remainingDp <= 0) return 0

    // +1 so the filler slightly exceeds the visible area rather than falling just short of it.
    val needed = (remainingDp / fillerRowHeightDp) + 1
    return needed.coerceIn(0, maxFillerRows)
}

private fun readHeightDp(context: Context, appWidgetId: Int): Int? {
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return null
    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    val heightDp = maxOf(
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
    )
    return heightDp.takeIf { it > 0 }
}
