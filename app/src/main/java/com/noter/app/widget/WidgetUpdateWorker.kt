package com.noter.app.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Direct pushes (repository writes call NoteWidgetProvider/TaskWidgetProvider.requestUpdate()
 * immediately) are the primary refresh path. This periodic job is only a fallback for the cases
 * that path can't cover — a push that got dropped by an OEM launcher's update throttling. 15
 * minutes is the shortest interval WorkManager allows for periodic work.
 */
class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NoteWidgetProvider.requestUpdate(applicationContext)
        TaskWidgetProvider.requestUpdate(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "widget_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
