package com.noter.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context

// Last successfully-read widget height, per widget id — reused if a refresh size read is empty.
private val lastKnownHeightDp = mutableMapOf<Int, Int>()

/**
 * How many blank filler rows a widget list needs so its content fills the widget's real height.
 * Short lists get padded to the actual size; the count falls back to 2 if the size is unknown.
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
