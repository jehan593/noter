package com.noter.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Direct pushes (repository writes call NoteWidgetProvider/TaskWidgetProvider.requestUpdate()
 * immediately) are the primary refresh path. This alarm is only a fallback for the cases that
 * path can't cover — a push that got dropped by an OEM launcher's update throttling.
 *
 * AlarmManager instead of WorkManager: the only thing this needs is "call requestUpdate() every
 * ~15 minutes," and WorkManager's own manifest unconditionally adds WAKE_LOCK,
 * ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED, and FOREGROUND_SERVICE permissions to every build
 * regardless of whether this job uses any of that — permissions this app otherwise doesn't need.
 * setInexactRepeating with ELAPSED_REALTIME (not ...WAKEUP) needs no extra permission, doesn't
 * wake a sleeping device for a refresh with no urgency, and lets the system batch it with other
 * apps' alarms. The tradeoff: without RECEIVE_BOOT_COMPLETED this fallback timer doesn't resume
 * until the app process next starts after a reboot — acceptable since it's only a fallback, and
 * the direct push path works immediately regardless.
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
