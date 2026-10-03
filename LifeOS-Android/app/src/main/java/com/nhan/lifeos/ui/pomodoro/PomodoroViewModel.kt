package com.nhan.lifeos.ui.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PomodoroUiState(
    val totalSeconds: Int = 25 * 60,
    val secondsLeft: Int = 25 * 60,
    val isRunning: Boolean = false,
    val isWorkMode: Boolean = true,
    val completedSessions: Int = 0,
    val totalMinutesFocused: Int = 0
) {
    val progress: Float
        get() = if (totalSeconds > 0) (1f - (secondsLeft.toFloat() / totalSeconds.toFloat())).coerceIn(0f, 1f) else 0f

    val formattedTime: String
        get() {
            val m = secondsLeft / 60
            val s = secondsLeft % 60
            return "%02d:%02d".format(m, s)
        }
}

class PomodoroViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun setTime(minutes: Int, isWork: Boolean = true) {
        timerJob?.cancel()
        val secs = minutes * 60
        _uiState.value = _uiState.value.copy(
            totalSeconds = secs,
            secondsLeft = secs,
            isRunning = false,
            isWorkMode = isWork
        )
    }

    fun toggleStartPause() {
        if (_uiState.value.isRunning) {
            pause()
        } else {
            start()
        }
    }

    private fun start() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(isRunning = true)
        timerJob = viewModelScope.launch {
            while (_uiState.value.secondsLeft > 0 && _uiState.value.isRunning) {
                delay(1000L)
                val left = _uiState.value.secondsLeft - 1
                if (left <= 0) {
                    val wasWork = _uiState.value.isWorkMode
                    val newSessions = if (wasWork) _uiState.value.completedSessions + 1 else _uiState.value.completedSessions
                    val newMins = if (wasWork) _uiState.value.totalMinutesFocused + (_uiState.value.totalSeconds / 60) else _uiState.value.totalMinutesFocused
                    _uiState.value = _uiState.value.copy(
                        secondsLeft = 0,
                        isRunning = false,
                        completedSessions = newSessions,
                        totalMinutesFocused = newMins
                    )
                    break
                } else {
                    _uiState.value = _uiState.value.copy(secondsLeft = left)
                }
            }
        }
    }

    fun pause() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(isRunning = false)
    }

    fun reset() {
        timerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            secondsLeft = _uiState.value.totalSeconds,
            isRunning = false
        )
    }
}
