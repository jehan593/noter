package com.noter.app.widget

import android.content.Context
import android.content.Intent
import com.noter.app.MainActivity

fun buildLaunchIntent(context: Context, tab: Int): Intent =
    Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_START_TAB, tab)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
