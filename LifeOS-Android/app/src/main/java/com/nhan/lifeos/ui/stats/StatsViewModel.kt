package com.nhan.lifeos.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.GoalEntity
import com.nhan.lifeos.data.local.entity.ProjectEntity
import com.nhan.lifeos.data.local.entity.TodoEntity
import com.nhan.lifeos.data.local.entity.TransactionEntity
import com.nhan.lifeos.data.repository.FinanceRepository
import com.nhan.lifeos.data.repository.TaskTimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class StatsUiState(
    val completedTodos: Int = 0,
    val pendingTodos: Int = 0,
    val totalTodos: Int = 0,
    val todoCompletionRate: Int = 0,
    val totalProjects: Int = 0,
    val doneProjects: Int = 0,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val savingsRate: Double = 0.0,
    val completedGoals: Int = 0,
    val totalGoals: Int = 0,
    val productivityScore: Int = 85,
    val isLoading: Boolean = false
)

class StatsViewModel(
    private val taskTimeRepository: TaskTimeRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState(isLoading = true))
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                taskTimeRepository.allTodos,
                taskTimeRepository.allProjects,
                financeRepository.getAllTransactions(),
                financeRepository.getAllGoals()
            ) { todos: List<TodoEntity>, projects: List<ProjectEntity>, txs: List<TransactionEntity>, goals: List<GoalEntity> ->
                val doneTodos = todos.count { it.done }
                val totalTodos = todos.size
                val todoRate = if (totalTodos > 0) ((doneTodos.toDouble() / totalTodos.toDouble()) * 100).toInt() else 0

                val doneProjects = projects.count { it.status == "done" || it.status == "Hoàn thành" }

                val inc = txs.filter { it.type == "income" }.sumOf { it.amount }
                val exp = txs.filter { it.type == "expense" }.sumOf { it.amount }
                val bal = inc - exp
                val savRate = if (inc > 0) ((bal.toDouble() / inc.toDouble()) * 100.0).coerceAtLeast(0.0) else 0.0

                val doneGoals = goals.count { it.isCompleted }

                // Score formula: 50% todo rate + 30% savings rate + 20% goals rate
                val goalsRate = if (goals.isNotEmpty()) (doneGoals.toDouble() / goals.size.toDouble()) * 100.0 else 50.0
                val score = ((todoRate * 0.5) + (savRate.coerceIn(0.0, 100.0) * 0.3) + (goalsRate * 0.2)).toInt().coerceIn(10, 100)

                StatsUiState(
                    completedTodos = doneTodos,
                    pendingTodos = totalTodos - doneTodos,
                    totalTodos = totalTodos,
                    todoCompletionRate = todoRate,
                    totalProjects = projects.size,
                    doneProjects = doneProjects,
                    totalIncome = inc,
                    totalExpense = exp,
                    netBalance = bal,
                    savingsRate = (savRate * 10).toLong() / 10.0,
                    completedGoals = doneGoals,
                    totalGoals = goals.size,
                    productivityScore = score,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
