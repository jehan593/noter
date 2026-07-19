package com.noter.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.noter.app.data.remote.NotesnookApi
import com.noter.app.widget.NoteWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class NotesnookSettings(val apiKey: String?, val tagId: String?)

class NotesRepository(
    private val dataStore: DataStore<Preferences>,
    private val context: Context
) {
    companion object {
        val NOTE_TEXT = stringPreferencesKey("note_text")
        val NOTESNOOK_API_KEY = stringPreferencesKey("notesnook_api_key")
        val NOTESNOOK_TAG_ID = stringPreferencesKey("notesnook_tag_id")
    }

    val noteFlow: Flow<String> = dataStore.data.map { it[NOTE_TEXT] ?: "" }

    val notesnookSettingsFlow: Flow<NotesnookSettings> = dataStore.data.map {
        NotesnookSettings(it[NOTESNOOK_API_KEY], it[NOTESNOOK_TAG_ID])
    }

    suspend fun saveNote(text: String) {
        dataStore.edit { it[NOTE_TEXT] = text }
        NoteWidgetProvider.requestUpdate(context)
    }

    suspend fun saveNotesnookSettings(apiKey: String, tagId: String) {
        dataStore.edit {
            if (apiKey.isBlank()) it.remove(NOTESNOOK_API_KEY) else it[NOTESNOOK_API_KEY] = apiKey
            if (tagId.isBlank()) it.remove(NOTESNOOK_TAG_ID) else it[NOTESNOOK_TAG_ID] = tagId
        }
    }

    suspend fun sendToNotesnook(text: String): Result<Unit> {
        val settings = notesnookSettingsFlow.first()
        val apiKey = settings.apiKey
        if (apiKey.isNullOrBlank()) {
            return Result.failure(IllegalStateException("No API key set"))
        }
        return NotesnookApi.sendNote(apiKey, text, settings.tagId)
    }
}
