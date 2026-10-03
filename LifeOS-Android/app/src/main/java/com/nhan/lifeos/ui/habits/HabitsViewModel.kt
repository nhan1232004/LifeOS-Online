package com.nhan.lifeos.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.HabitEntity
import com.nhan.lifeos.data.repository.PersonalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HabitsUiState(
    val habits: List<HabitEntity> = emptyList(),
    val totalStreak: Int = 0,
    val isLoading: Boolean = false
)

class HabitsViewModel(private val repository: PersonalRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitsUiState(isLoading = true))
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allHabits.collect { list ->
                val streakSum = list.sumOf { it.streak }
                _uiState.value = HabitsUiState(
                    habits = list,
                    totalStreak = streakSum,
                    isLoading = false
                )
            }
        }
    }

    fun toggleDay(id: String, date: String) {
        viewModelScope.launch {
            repository.toggleHabitDay(id, date)
        }
    }

    fun addHabit(name: String, target: String) {
        viewModelScope.launch {
            repository.insertHabit(name, target)
        }
    }

    fun deleteHabit(id: String) {
        viewModelScope.launch {
            repository.deleteHabit(id)
        }
    }
}
