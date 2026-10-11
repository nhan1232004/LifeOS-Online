package com.nhan.lifeos.ui.pomodoro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.TodoEntity
import com.nhan.lifeos.data.repository.TaskTimeRepository
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
    val totalMinutesFocused: Int = 0,
    val activeTaskId: String? = null,
    val activeTaskTitle: String? = null,
    val pendingTodos: List<TodoEntity> = emptyList()
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

class PomodoroViewModel(
    private val taskTimeRepo: TaskTimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PomodoroUiState())
    val uiState: StateFlow<PomodoroUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            taskTimeRepo.allTodos.collect { todos ->
                _uiState.value = _uiState.value.copy(
                    pendingTodos = todos.filter { !it.done }
                )
            }
        }
    }

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

    /**
     * Toggles task selection for Pomodoro focus.
     * If the task is already active, it safely UNSELECTS it (no stacking/overlapping).
     * If another task is chosen, it switches cleanly.
     */
    fun toggleSelectTask(task: TodoEntity) {
        if (_uiState.value.activeTaskId == task.id) {
            _uiState.value = _uiState.value.copy(
                activeTaskId = null,
                activeTaskTitle = null
            )
        } else {
            _uiState.value = _uiState.value.copy(
                activeTaskId = task.id,
                activeTaskTitle = task.text
            )
        }
    }

    fun unselectTask() {
        _uiState.value = _uiState.value.copy(
            activeTaskId = null,
            activeTaskTitle = null
        )
    }

    fun completeActiveTask() {
        val taskId = _uiState.value.activeTaskId ?: return
        viewModelScope.launch {
            taskTimeRepo.toggleTodo(taskId, true)
            unselectTask()
        }
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
