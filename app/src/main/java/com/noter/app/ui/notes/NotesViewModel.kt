package com.noter.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noter.app.data.repository.NotesRepository
import com.noter.app.data.repository.NotesnookSettings
import kotlinx.coroutines.delay
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

    private val _notesnookSettings = MutableStateFlow(NotesnookSettings(apiKey = null, tagId = null))
    val notesnookSettings: StateFlow<NotesnookSettings> = _notesnookSettings.asStateFlow()

    private val _isSettingsDialogOpen = MutableStateFlow(false)
    val isSettingsDialogOpen: StateFlow<Boolean> = _isSettingsDialogOpen.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Mirrors the extension's flashStatus(): shows a short-lived message, then clears itself
    // unless a newer message has already replaced it.
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

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
        viewModelScope.launch {
            repository.notesnookSettingsFlow.collectLatest { _notesnookSettings.value = it }
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

    fun openSettingsDialog() {
        _isSettingsDialogOpen.value = true
    }

    fun dismissSettingsDialog() {
        _isSettingsDialogOpen.value = false
    }

    fun saveNotesnookSettings(apiKey: String, tagId: String) {
        viewModelScope.launch {
            repository.saveNotesnookSettings(apiKey.trim(), tagId.trim())
            _isSettingsDialogOpen.value = false
        }
    }

    // Network I/O can't run blocking on the main thread the way the local-only actions in
    // TasksViewModel do (Android throws on that outright), so this is a plain launch — if the app
    // gets backgrounded and frozen mid-request, the send simply doesn't complete, same as it
    // wouldn't in the browser extension this mirrors.
    fun sendToNotesnook() {
        val text = _noteText.value
        if (text.isBlank()) {
            flashStatus("Nothing to send")
            return
        }
        if (_notesnookSettings.value.apiKey.isNullOrBlank()) {
            flashStatus("No API key set")
            openSettingsDialog()
            return
        }
        viewModelScope.launch {
            _isSending.value = true
            val result = repository.sendToNotesnook(text)
            _isSending.value = false
            flashStatus(if (result.isSuccess) "Sent to Notesnook" else "Send failed")
        }
    }

    private fun flashStatus(message: String) {
        viewModelScope.launch {
            _statusMessage.value = message
            delay(1500)
            if (_statusMessage.value == message) _statusMessage.value = null
        }
    }
}
