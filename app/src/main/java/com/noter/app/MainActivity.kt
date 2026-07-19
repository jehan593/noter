package com.noter.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.noter.app.ui.notes.NotesScreen
import com.noter.app.ui.tasks.TasksScreen
import com.noter.app.ui.theme.NoterTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_START_TAB = "com.noter.app.EXTRA_START_TAB"
        const val TAB_NOTES = 0
        const val TAB_TASKS = 1
    }

    private var selectedTab by mutableIntStateOf(TAB_NOTES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedTab = intent.getIntExtra(EXTRA_START_TAB, TAB_NOTES)

        setContent {
            NoterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NoterApp(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        selectedTab = intent.getIntExtra(EXTRA_START_TAB, selectedTab)
    }
}

@Composable
private fun NoterApp(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Scaffold(
        topBar = {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.statusBarsPadding()
            ) {
                Tab(
                    selected = selectedTab == MainActivity.TAB_NOTES,
                    onClick = { onTabSelected(MainActivity.TAB_NOTES) },
                    text = { Text("Notes") }
                )
                Tab(
                    selected = selectedTab == MainActivity.TAB_TASKS,
                    onClick = { onTabSelected(MainActivity.TAB_TASKS) },
                    text = { Text("Tasks") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                MainActivity.TAB_NOTES -> NotesScreen()
                else -> TasksScreen()
            }
        }
    }
}
