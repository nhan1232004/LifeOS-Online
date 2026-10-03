package com.nhan.lifeos.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.GoalEntity
import com.nhan.lifeos.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GoalsUiState(
    val goals: List<GoalEntity> = emptyList(),
    val totalGoals: Int = 0,
    val completedGoals: Int = 0,
    val inProgressGoals: Int = 0,
    val isLoading: Boolean = false
)

class GoalsViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState(isLoading = true))
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllGoals().collect { list ->
                val completed = list.count { it.isCompleted }
                _uiState.value = GoalsUiState(
                    goals = list,
                    totalGoals = list.size,
                    completedGoals = completed,
                    inProgressGoals = list.size - completed,
                    isLoading = false
                )
            }
        }
    }

    fun addGoal(
        title: String,
        category: String,
        targetValue: Long,
        currentValue: Long,
        unit: String,
        deadline: String,
        note: String
    ) {
        viewModelScope.launch {
            repository.addGoal(
                title = title,
                category = category,
                targetValue = targetValue,
                currentValue = currentValue,
                unit = unit,
                deadline = deadline,
                note = note
            )
        }
    }

    fun addToProgress(id: String, amount: Long) {
        viewModelScope.launch {
            repository.addToGoalProgress(id, amount)
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }
}
