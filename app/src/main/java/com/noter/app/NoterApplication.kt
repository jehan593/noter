package com.noter.app

import android.app.Application
import com.noter.app.di.AppContainer
import com.noter.app.di.DefaultAppContainer
import com.noter.app.widget.WidgetUpdateWorker

class NoterApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        WidgetUpdateWorker.schedule(this)
    }
}
