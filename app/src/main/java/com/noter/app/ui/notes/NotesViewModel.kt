package com.noter.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noter.app.data.repository.NotesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NotesViewModel(private val repository: NotesRepository) : ViewModel() {

    private val _noteText = MutableStateFlow("")
    val noteText: StateFlow<String> = _noteText.asStateFlow()

    // Short-lived message shown under the buttons (currently always empty, kept for the layout).
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Autosaves are debounced. A synchronous flush also runs on app pause, so the last
    // keystrokes always reach the saved note (and the widget) even if the app is backgrounded.
    private val saveRequests = MutableSharedFlow<String>(extraBufferCapacity = 64)

    init {
        viewModelScope.launch { _noteText.value = repository.noteFlow.first() }
        viewModelScope.launch {
            saveRequests.collectLatest { repository.saveNote(it) }
        }
    }

    fun onTextChange(text: String) {
        _noteText.value = text
        saveRequests.tryEmit(text)
    }

    fun onClear() {
        _noteText.value = ""
        runBlocking { repository.saveNote("") }
    }

    fun flushPendingSaveBlocking() {
        runBlocking { repository.saveNote(_noteText.value) }
    }
}