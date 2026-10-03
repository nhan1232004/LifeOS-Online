package com.nhan.lifeos.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.EventEntity
import com.nhan.lifeos.data.local.entity.TodoEntity
import com.nhan.lifeos.data.repository.TaskTimeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TodayUiState(
    val currentDateFormatted: String = "",
    val todayDateRaw: String = "",
    val events: List<EventEntity> = emptyList(),
    val todos: List<TodoEntity> = emptyList(),
    val completedCount: Int = 0,
    val totalTodoCount: Int = 0,
    val productivityPercentage: Int = 0
)

class TodayViewModel(private val repository: TaskTimeRepository) : ViewModel() {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayRaw: String = sdf.format(Date())

    val uiState: StateFlow<TodayUiState> = combine(
        repository.getEventsForDate(todayRaw),
        repository.getTodosForDate(todayRaw)
    ) { events, todos ->
        val dateFormat = SimpleDateFormat("EEEE, 'ngày' dd 'tháng' MM", Locale("vi", "VN"))
        val dateFormatted = dateFormat.format(Date()).replaceFirstChar { it.uppercase() }

        val completed = todos.count { it.done }
        val total = todos.size
        val percent = if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 100

        TodayUiState(
            currentDateFormatted = dateFormatted,
            todayDateRaw = todayRaw,
            events = events,
            todos = todos,
            completedCount = completed,
            totalTodoCount = total,
            productivityPercentage = percent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodayUiState(todayDateRaw = todayRaw)
    )

    fun toggleTodo(id: String, done: Boolean) {
        viewModelScope.launch {
            repository.toggleTodo(id, done)
        }
    }

    fun addQuickTodo(text: String, priority: String) {
        viewModelScope.launch {
            repository.insertTodo(
                text = text,
                priority = priority,
                date = todayRaw
            )
        }
    }
}
