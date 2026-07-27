package com.vano.n8nmobile.logging

import androidx.compose.runtime.mutableStateListOf

data class LogEntry(val timestamp: Long, val tag: String, val message: String)

object AppLog {
    private val _entries = mutableStateListOf<LogEntry>()
    val entries: List<LogEntry> get() = _entries

    fun add(tag: String, message: String) {
        _entries.add(0, LogEntry(System.currentTimeMillis(), tag, message))
        if (_entries.size > 200) {
            _entries.removeAt(_entries.size - 1)
        }
    }

    fun exportText(): String {
        if (_entries.isEmpty()) return ""
        return _entries.reversed().joinToString("\n") { entry -> "[${entry.tag}] ${entry.message}" }
    }
}
