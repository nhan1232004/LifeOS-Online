package com.nhan.lifeos.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.EventEntity
import com.nhan.lifeos.data.local.entity.TodoEntity
import com.nhan.lifeos.data.repository.FinanceRepository
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
    val productivityPercentage: Int = 0,
    val totalBalance: Long = 0L,
    val monthIncome: Long = 0L,
    val monthExpense: Long = 0L,
    val netSavingsMonth: Long = 0L,
    val savingsRate: Int = 0
)

class TodayViewModel(
    private val repository: TaskTimeRepository,
    private val financeRepository: FinanceRepository
) : ViewModel() {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayRaw: String = sdf.format(Date())

    val uiState: StateFlow<TodayUiState> = combine(
        repository.getEventsForDate(todayRaw),
        repository.getTodosForDate(todayRaw),
        financeRepository.getAllTransactions()
    ) { events, todos, transactions ->
        val dateFormat = SimpleDateFormat("EEEE, 'ngày' dd 'tháng' MM", Locale("vi", "VN"))
        val dateFormatted = dateFormat.format(Date()).replaceFirstChar { it.uppercase() }

        val completed = todos.count { it.done }
        val total = todos.size
        val percent = if (total > 0) ((completed.toFloat() / total) * 100).toInt() else 100

        // Calculate real savings from DB transactions
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        var totalIncome = 0L
        var totalExpense = 0L
        var mIncome = 0L
        var mExpense = 0L

        transactions.forEach { tx ->
            if (tx.type.equals("income", ignoreCase = true)) {
                totalIncome += tx.amount
                if (tx.date.startsWith(currentMonthPrefix)) {
                    mIncome += tx.amount
                }
            } else {
                totalExpense += tx.amount
                if (tx.date.startsWith(currentMonthPrefix)) {
                    mExpense += tx.amount
                }
            }
        }

        val balance = totalIncome - totalExpense
        val netMonthSavings = mIncome - mExpense
        val sRate = if (mIncome > 0) {
            ((netMonthSavings.toFloat() / mIncome.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else if (balance > 0) {
            100
        } else {
            0
        }

        TodayUiState(
            currentDateFormatted = dateFormatted,
            todayDateRaw = todayRaw,
            events = events,
            todos = todos,
            completedCount = completed,
            totalTodoCount = total,
            productivityPercentage = percent,
            totalBalance = balance,
            monthIncome = mIncome,
            monthExpense = mExpense,
            netSavingsMonth = netMonthSavings,
            savingsRate = sRate
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
