package com.noter.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Periodic backup refresh (~15 min) in case a direct widget update was dropped. Uses
 * AlarmManager because WorkManager would add permissions this app doesn't need.
 */
class WidgetRefreshAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NoteWidgetProvider.requestUpdate(context)
        TaskWidgetProvider.requestUpdate(context)
    }

    companion object {
        private const val INTERVAL_MILLIS = 15 * 60 * 1000L
        private const val REQUEST_CODE = 2001

        fun schedule(context: Context) {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, WidgetRefreshAlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + INTERVAL_MILLIS,
                INTERVAL_MILLIS,
                pendingIntent
            )
        }
    }
}
