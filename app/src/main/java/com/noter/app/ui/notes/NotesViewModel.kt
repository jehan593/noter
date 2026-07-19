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

    // Typing stays async (collectLatest) so keystrokes never feel blocked. But Android can freeze
    // a backgrounded app's coroutines mid-flight the instant it's minimized, so an in-flight save
    // triggered by the last keystroke can be frozen before it (and the widget refresh nested
    // inside it) ever completes. flushPendingSaveBlocking() is the guaranteed-complete fallback,
    // called synchronously from ON_PAUSE — the last point guaranteed to run before that freeze.
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
