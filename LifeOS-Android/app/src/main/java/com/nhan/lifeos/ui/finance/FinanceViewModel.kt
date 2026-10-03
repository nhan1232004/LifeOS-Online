package com.nhan.lifeos.ui.finance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.TransactionEntity
import com.nhan.lifeos.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TransactionFilter {
    ALL, EXPENSE, INCOME
}

data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val currentFilter: TransactionFilter = TransactionFilter.ALL,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val savingsRate: Double = 0.0,
    val categoryExpenses: Map<String, Long> = emptyMap(),
    val isLoading: Boolean = false
)

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(FinanceUiState(isLoading = true))
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private var allTransactions: List<TransactionEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            repository.getAllTransactions().collect { list ->
                allTransactions = list
                calculateAndEmit()
            }
        }
    }

    fun setFilter(filter: TransactionFilter) {
        _uiState.value = _uiState.value.copy(currentFilter = filter)
        calculateAndEmit()
    }

    fun addTransaction(
        type: String,
        categoryOrSource: String,
        amount: Long,
        date: String,
        paymentMethod: String,
        note: String
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                type = type,
                categoryOrSource = categoryOrSource,
                amount = amount,
                date = date,
                paymentMethod = paymentMethod,
                note = note
            )
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun getCsvData(): String {
        return repository.generateTransactionsCsv(allTransactions)
    }

    private fun calculateAndEmit() {
        val incomeTxs = allTransactions.filter { it.type == "income" }
        val expenseTxs = allTransactions.filter { it.type == "expense" }

        val totalInc = incomeTxs.sumOf { it.amount }
        val totalExp = expenseTxs.sumOf { it.amount }
        val balance = totalInc - totalExp
        val rate = if (totalInc > 0) ((balance.toDouble() / totalInc.toDouble()) * 100.0).coerceAtLeast(0.0) else 0.0

        val categoryMap = mutableMapOf<String, Long>()
        expenseTxs.forEach { tx ->
            val cat = tx.categoryOrSource.ifBlank { "Khác" }
            categoryMap[cat] = (categoryMap[cat] ?: 0L) + tx.amount
        }

        val filtered = when (_uiState.value.currentFilter) {
            TransactionFilter.ALL -> allTransactions
            TransactionFilter.EXPENSE -> expenseTxs
            TransactionFilter.INCOME -> incomeTxs
        }

        _uiState.value = _uiState.value.copy(
            transactions = filtered,
            totalIncome = totalInc,
            totalExpense = totalExp,
            netBalance = balance,
            savingsRate = (rate * 10).toLong() / 10.0, // rounded to 1 decimal
            categoryExpenses = categoryMap.toList().sortedByDescending { it.second }.toMap(),
            isLoading = false
        )
    }
}
