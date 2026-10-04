package com.nhan.lifeos.ui.todos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.TodoEntity
import com.nhan.lifeos.data.repository.TaskTimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TodoFilter {
    ALL, PENDING, COMPLETED
}

data class TodosUiState(
    val todos: List<TodoEntity> = emptyList(),
    val currentFilter: TodoFilter = TodoFilter.ALL,
    val pendingCount: Int = 0,
    val completedCount: Int = 0
)

class TodosViewModel(private val repository: TaskTimeRepository) : ViewModel() {

    private val _filter = MutableStateFlow(TodoFilter.ALL)

    val uiState: StateFlow<TodosUiState> = combine(
        repository.allTodos,
        _filter
    ) { allTodos, filter ->
        val filtered = when (filter) {
            TodoFilter.ALL -> allTodos
            TodoFilter.PENDING -> allTodos.filter { !it.done }
            TodoFilter.COMPLETED -> allTodos.filter { it.done }
        }
        TodosUiState(
            todos = filtered,
            currentFilter = filter,
            pendingCount = allTodos.count { !it.done },
            completedCount = allTodos.count { it.done }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TodosUiState()
    )

    fun setFilter(filter: TodoFilter) {
        _filter.value = filter
    }

    fun toggleTodo(id: String, done: Boolean) {
        viewModelScope.launch {
            repository.toggleTodo(id, done)
        }
    }

    fun deleteTodo(id: String) {
        viewModelScope.launch {
            repository.deleteTodo(id)
        }
    }

    fun updateTodo(todo: TodoEntity) {
        viewModelScope.launch {
            repository.updateTodo(todo)
        }
    }

    fun addTodo(text: String, priority: String, date: String = "", note: String = "") {
        viewModelScope.launch {
            val taskDate = if (date.isBlank()) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            } else date
            repository.insertTodo(
                text = text,
                priority = priority,
                date = taskDate,
                note = note
            )
        }
    }
}
