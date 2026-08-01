package com.example.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class LogLevel {
    INFO, SUCCESS, WARNING, ERROR
}

data class LogItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val details: String? = null
)

object AppLogger {
    private const val MAX_LOGS = 300
    private val _logs = MutableStateFlow<List<LogItem>>(emptyList())
    val logs: StateFlow<List<LogItem>> = _logs.asStateFlow()

    @Synchronized
    private fun addLog(level: LogLevel, tag: String, message: String, details: String? = null) {
        val item = LogItem(level = level, tag = tag, message = message, details = details)
        
        // Print to standard Logcat
        val formattedMsg = if (details != null) "$message | Details: $details" else message
        when (level) {
            LogLevel.INFO -> Log.i(tag, formattedMsg)
            LogLevel.SUCCESS -> Log.i(tag, "✅ [SUCCESS] $formattedMsg")
            LogLevel.WARNING -> Log.w(tag, formattedMsg)
            LogLevel.ERROR -> Log.e(tag, formattedMsg)
        }

        // Keep in-memory list for UI Debugger
        val currentList = _logs.value.toMutableList()
        currentList.add(0, item) // Add latest at top
        if (currentList.size > MAX_LOGS) {
            currentList.removeAt(currentList.lastIndex)
        }
        _logs.value = currentList
    }

    fun i(tag: String, message: String, details: String? = null) {
        addLog(LogLevel.INFO, tag, message, details)
    }

    fun s(tag: String, message: String, details: String? = null) {
        addLog(LogLevel.SUCCESS, tag, message, details)
    }

    fun w(tag: String, message: String, details: String? = null) {
        addLog(LogLevel.WARNING, tag, message, details)
    }

    fun e(tag: String, message: String, details: String? = null, throwable: Throwable? = null) {
        val fullDetails = if (throwable != null) {
            "${details ?: ""}\nException: ${throwable.localizedMessage}\n${Log.getStackTraceString(throwable)}"
        } else {
            details
        }
        addLog(LogLevel.ERROR, tag, message, fullDetails)
    }

    fun clear() {
        _logs.value = emptyList()
        addLog(LogLevel.INFO, "AppLogger", "Log buffer dibersihkan")
    }
}
