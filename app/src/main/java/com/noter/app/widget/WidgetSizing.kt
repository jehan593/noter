package com.noter.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context

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
 * If the size genuinely can't be read (very first frame before the widget host has reported
 * anything), this deliberately falls back to a SMALL number rather than a generous one — under-
 * filling only brings back a narrow slice of the original dead-space issue in a rare edge case,
 * while over-filling directly reintroduces unwanted scroll, which is the worse regression.
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
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return fallbackFillerRows

    val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
    val heightDp = maxOf(
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0),
        options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
    )
    if (heightDp <= 0) return fallbackFillerRows

    val availableForListDp = heightDp - overheadDp
    val remainingDp = availableForListDp - contentHeightDp
    if (remainingDp <= 0) return 0

    // +1 so the filler slightly exceeds the visible area rather than falling just short of it.
    val needed = (remainingDp / fillerRowHeightDp) + 1
    return needed.coerceIn(0, maxFillerRows)
}
