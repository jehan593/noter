package com.noter.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.noter.app.widget.NoteWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NotesRepository(
    private val dataStore: DataStore<Preferences>,
    private val context: Context
) {
    companion object {
        val NOTE_TEXT = stringPreferencesKey("note_text")
    }

    val noteFlow: Flow<String> = dataStore.data.map { it[NOTE_TEXT] ?: "" }

    suspend fun saveNote(text: String) {
        dataStore.edit { it[NOTE_TEXT] = text }
        NoteWidgetProvider.requestUpdate(context)
    }
}
