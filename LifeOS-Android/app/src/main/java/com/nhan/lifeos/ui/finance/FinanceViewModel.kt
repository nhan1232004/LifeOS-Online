package com.nhan.lifeos.ui.finance

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.TransactionEntity
import com.nhan.lifeos.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

enum class TransactionFilter {
    ALL, EXPENSE, INCOME
}

enum class FinanceTimePeriod(val label: String) {
    DAY("Ngày"),
    WEEK("Tuần"),
    MONTH("Tháng"),
    YEAR("Năm"),
    ALL("Tất cả"),
    CUSTOM("Tùy chọn")
}

data class FinanceSubOption(
    val key: String,
    val label: String
)

data class FinanceUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val periodTransactions: List<TransactionEntity> = emptyList(),
    val currentFilter: TransactionFilter = TransactionFilter.ALL,
    val selectedPeriod: FinanceTimePeriod = FinanceTimePeriod.MONTH,
    val selectedSubKey: String = "thisMonth",
    val subOptions: List<FinanceSubOption> = emptyList(),
    val startDate: String = "",
    val endDate: String = "",
    val dateRangeLabel: String = "",
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netBalance: Long = 0L,
    val savingsRate: Double = 0.0,
    val categoryExpenses: Map<String, Long> = emptyMap(),
    val sourceIncomes: Map<String, Long> = emptyMap(),
    val incomeCount: Int = 0,
    val expenseCount: Int = 0,
    val isLoading: Boolean = false
)

class FinanceViewModel(private val repository: FinanceRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FinanceUiState(
            isLoading = true,
            subOptions = getSubOptionsForPeriod(FinanceTimePeriod.MONTH)
        )
    )
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    private var allTransactions: List<TransactionEntity> = emptyList()

    init {
        // Initialize date intervals for default (Month -> This Month)
        computeDateInterval(FinanceTimePeriod.MONTH, "thisMonth")

        viewModelScope.launch {
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

    fun setTimePeriod(period: FinanceTimePeriod, subKey: String? = null) {
        val options = getSubOptionsForPeriod(period)
        val chosenKey = subKey ?: options.firstOrNull()?.key ?: "all"
        computeDateInterval(period, chosenKey)
        _uiState.value = _uiState.value.copy(
            selectedPeriod = period,
            selectedSubKey = chosenKey,
            subOptions = options
        )
        calculateAndEmit()
    }

    fun setSubRange(subKey: String) {
        computeDateInterval(_uiState.value.selectedPeriod, subKey)
        _uiState.value = _uiState.value.copy(selectedSubKey = subKey)
        calculateAndEmit()
    }

    fun setCustomRange(start: String, end: String) {
        val displayStart = if (start.isNotBlank()) start else LocalDate.now().toString()
        val displayEnd = if (end.isNotBlank()) end else LocalDate.now().toString()
        _uiState.value = _uiState.value.copy(
            selectedPeriod = FinanceTimePeriod.CUSTOM,
            selectedSubKey = "custom",
            startDate = displayStart,
            endDate = displayEnd,
            dateRangeLabel = "Tùy chọn (${formatShortDate(displayStart)} – ${formatShortDate(displayEnd)})",
            subOptions = emptyList()
        )
        calculateAndEmit()
    }

    fun exportExcel(context: Context, periodOnly: Boolean = true) {
        val listToExport = if (periodOnly) {
            _uiState.value.periodTransactions
        } else {
            allTransactions
        }
        val label = if (periodOnly) {
            _uiState.value.dateRangeLabel.ifBlank { "Kỳ đang xem" }
        } else {
            "Toàn bộ thời gian"
        }
        val start = if (periodOnly) _uiState.value.startDate else "1970-01-01"
        val end = if (periodOnly) _uiState.value.endDate else "2099-12-31"

        FinanceExcelExporter.exportAndShare(
            context = context,
            transactions = listToExport,
            label = label,
            startDate = start,
            endDate = end
        )
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

    fun updateTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun getCsvData(): String {
        return repository.generateTransactionsCsv(_uiState.value.periodTransactions)
    }

    private fun computeDateInterval(period: FinanceTimePeriod, subKey: String) {
        val today = LocalDate.now()
        val y = today.year
        val m = today.monthValue
        val shortFmt = DateTimeFormatter.ofPattern("dd/MM")

        var start = ""
        var end = ""
        var label = ""

        when (period) {
            FinanceTimePeriod.DAY -> {
                when (subKey) {
                    "today" -> {
                        start = today.toString()
                        end = today.toString()
                        label = "Hôm nay (${today.format(shortFmt)})"
                    }
                    "yesterday" -> {
                        val yest = today.minusDays(1)
                        start = yest.toString()
                        end = yest.toString()
                        label = "Hôm qua (${yest.format(shortFmt)})"
                    }
                    "last7days" -> {
                        val s = today.minusDays(6)
                        start = s.toString()
                        end = today.toString()
                        label = "7 ngày qua (${s.format(shortFmt)} – ${today.format(shortFmt)})"
                    }
                    "last14days" -> {
                        val s = today.minusDays(13)
                        start = s.toString()
                        end = today.toString()
                        label = "14 ngày qua (${s.format(shortFmt)} – ${today.format(shortFmt)})"
                    }
                    else -> { // last30days
                        val s = today.minusDays(29)
                        start = s.toString()
                        end = today.toString()
                        label = "30 ngày qua (${s.format(shortFmt)} – ${today.format(shortFmt)})"
                    }
                }
            }
            FinanceTimePeriod.WEEK -> {
                val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunday = monday.plusDays(6)
                when (subKey) {
                    "thisWeek" -> {
                        start = monday.toString()
                        end = sunday.toString()
                        label = "Tuần này (${monday.format(shortFmt)} – ${sunday.format(shortFmt)})"
                    }
                    "lastWeek" -> {
                        val lastMon = monday.minusWeeks(1)
                        val lastSun = lastMon.plusDays(6)
                        start = lastMon.toString()
                        end = lastSun.toString()
                        label = "Tuần trước (${lastMon.format(shortFmt)} – ${lastSun.format(shortFmt)})"
                    }
                    "last8weeks" -> {
                        val s = today.minusDays(55)
                        start = s.toString()
                        end = today.toString()
                        label = "8 tuần qua (${s.format(shortFmt)} – ${today.format(shortFmt)})"
                    }
                    else -> { // last4weeks
                        val s = today.minusDays(27)
                        start = s.toString()
                        end = today.toString()
                        label = "4 tuần qua (${s.format(shortFmt)} – ${today.format(shortFmt)})"
                    }
                }
            }
            FinanceTimePeriod.MONTH -> {
                when (subKey) {
                    "thisMonth" -> {
                        start = today.with(TemporalAdjusters.firstDayOfMonth()).toString()
                        end = today.with(TemporalAdjusters.lastDayOfMonth()).toString()
                        label = "Tháng $m/$y"
                    }
                    "lastMonth" -> {
                        val prev = today.minusMonths(1)
                        start = prev.with(TemporalAdjusters.firstDayOfMonth()).toString()
                        end = prev.with(TemporalAdjusters.lastDayOfMonth()).toString()
                        label = "Tháng ${prev.monthValue}/${prev.year}"
                    }
                    "last3months" -> {
                        start = today.minusMonths(2).with(TemporalAdjusters.firstDayOfMonth()).toString()
                        end = today.with(TemporalAdjusters.lastDayOfMonth()).toString()
                        label = "3 tháng qua (Quý)"
                    }
                    "last6months" -> {
                        start = today.minusMonths(5).with(TemporalAdjusters.firstDayOfMonth()).toString()
                        end = today.with(TemporalAdjusters.lastDayOfMonth()).toString()
                        label = "6 tháng qua"
                    }
                    else -> { // last12months
                        start = today.minusMonths(11).with(TemporalAdjusters.firstDayOfMonth()).toString()
                        end = today.with(TemporalAdjusters.lastDayOfMonth()).toString()
                        label = "12 tháng qua"
                    }
                }
            }
            FinanceTimePeriod.YEAR -> {
                when (subKey) {
                    "lastYear" -> {
                        start = "${y - 1}-01-01"
                        end = "${y - 1}-12-31"
                        label = "Năm ${y - 1}"
                    }
                    else -> { // thisYear
                        start = "$y-01-01"
                        end = "$y-12-31"
                        label = "Năm $y"
                    }
                }
            }
            FinanceTimePeriod.ALL -> {
                start = "1970-01-01"
                end = "2099-12-31"
                label = "Toàn bộ thời gian"
            }
            FinanceTimePeriod.CUSTOM -> {
                if (_uiState.value.startDate.isNotBlank()) {
                    start = _uiState.value.startDate
                    end = _uiState.value.endDate
                    label = _uiState.value.dateRangeLabel
                } else {
                    start = today.with(TemporalAdjusters.firstDayOfMonth()).toString()
                    end = today.with(TemporalAdjusters.lastDayOfMonth()).toString()
                    label = "Tùy chọn"
                }
            }
        }

        _uiState.value = _uiState.value.copy(
            startDate = start,
            endDate = end,
            dateRangeLabel = label
        )
    }

    private fun calculateAndEmit() {
        val start = _uiState.value.startDate
        val end = _uiState.value.endDate

        // Filter transactions strictly by selected date interval
        val inPeriodTxs = allTransactions.filter { tx ->
            (start.isBlank() || tx.date >= start) && (end.isBlank() || tx.date <= end)
        }

        val incomeTxs = inPeriodTxs.filter { it.type == "income" }
        val expenseTxs = inPeriodTxs.filter { it.type == "expense" }

        val totalInc = incomeTxs.sumOf { it.amount }
        val totalExp = expenseTxs.sumOf { it.amount }
        val balance = totalInc - totalExp
        val rate = if (totalInc > 0) ((balance.toDouble() / totalInc.toDouble()) * 100.0).coerceAtLeast(0.0) else 0.0

        val categoryExpenseMap = mutableMapOf<String, Long>()
        expenseTxs.forEach { tx ->
            val cat = tx.categoryOrSource.ifBlank { "Khác" }
            categoryExpenseMap[cat] = (categoryExpenseMap[cat] ?: 0L) + tx.amount
        }

        val sourceIncomeMap = mutableMapOf<String, Long>()
        incomeTxs.forEach { tx ->
            val src = tx.categoryOrSource.ifBlank { "Khác" }
            sourceIncomeMap[src] = (sourceIncomeMap[src] ?: 0L) + tx.amount
        }

        // Apply display type filter (ALL / EXPENSE / INCOME)
        val filteredForDisplay = when (_uiState.value.currentFilter) {
            TransactionFilter.ALL -> inPeriodTxs
            TransactionFilter.EXPENSE -> expenseTxs
            TransactionFilter.INCOME -> incomeTxs
        }.sortedByDescending { it.date }

        _uiState.value = _uiState.value.copy(
            transactions = filteredForDisplay,
            periodTransactions = inPeriodTxs,
            totalIncome = totalInc,
            totalExpense = totalExp,
            netBalance = balance,
            savingsRate = (rate * 10).toLong() / 10.0,
            categoryExpenses = categoryExpenseMap.toList().sortedByDescending { it.second }.toMap(),
            sourceIncomes = sourceIncomeMap.toList().sortedByDescending { it.second }.toMap(),
            incomeCount = incomeTxs.size,
            expenseCount = expenseTxs.size,
            isLoading = false
        )
    }

    private fun formatShortDate(d: String): String {
        if (d.isBlank()) return ""
        val parts = d.split("-")
        return if (parts.size == 3) "${parts[2]}/${parts[1]}" else d
    }

    private fun getSubOptionsForPeriod(period: FinanceTimePeriod): List<FinanceSubOption> {
        val y = LocalDate.now().year
        return when (period) {
            FinanceTimePeriod.DAY -> listOf(
                FinanceSubOption("today", "Hôm nay"),
                FinanceSubOption("yesterday", "Hôm qua"),
                FinanceSubOption("last7days", "7 ngày qua"),
                FinanceSubOption("last14days", "14 ngày qua"),
                FinanceSubOption("last30days", "30 ngày qua")
            )
            FinanceTimePeriod.WEEK -> listOf(
                FinanceSubOption("thisWeek", "Tuần này"),
                FinanceSubOption("lastWeek", "Tuần trước"),
                FinanceSubOption("last4weeks", "4 tuần qua"),
                FinanceSubOption("last8weeks", "8 tuần qua")
            )
            FinanceTimePeriod.MONTH -> listOf(
                FinanceSubOption("thisMonth", "Tháng này"),
                FinanceSubOption("lastMonth", "Tháng trước"),
                FinanceSubOption("last3months", "3 tháng qua (Quý)"),
                FinanceSubOption("last6months", "6 tháng qua"),
                FinanceSubOption("last12months", "12 tháng qua")
            )
            FinanceTimePeriod.YEAR -> listOf(
                FinanceSubOption("thisYear", "Năm $y"),
                FinanceSubOption("lastYear", "Năm ${y - 1}")
            )
            FinanceTimePeriod.ALL -> listOf(
                FinanceSubOption("all", "Toàn bộ thời gian")
            )
            FinanceTimePeriod.CUSTOM -> emptyList()
        }
    }
}
