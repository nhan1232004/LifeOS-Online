package com.nhan.lifeos.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.EventEntity
import com.nhan.lifeos.data.repository.TaskTimeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthDay(
    val dateString: String, // "YYYY-MM-DD"
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val eventCount: Int = 0
)

data class CalendarUiState(
    val selectedDate: String,
    val viewMode: String = "month", // "week" or "month"
    val monthTitle: String = "",
    val monthDays: List<MonthDay> = emptyList(),
    val eventsForSelectedDate: List<EventEntity> = emptyList(),
    val allEvents: List<EventEntity> = emptyList()
)

class CalendarViewModel(private val repository: TaskTimeRepository) : ViewModel() {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val _selectedDate = MutableStateFlow(sdf.format(Date()))
    private val _viewMode = MutableStateFlow("month")

    // Calendar navigation (Year & Month)
    private val calendarInstance = Calendar.getInstance()
    private val _currentYearMonth = MutableStateFlow(
        Pair(calendarInstance.get(Calendar.YEAR), calendarInstance.get(Calendar.MONTH))
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedDate,
        _viewMode,
        _currentYearMonth,
        _selectedDate.flatMapLatest { date -> repository.getEventsForDate(date) },
        repository.allEvents
    ) { date, mode, yearMonth, dayEvents, allEvs ->
        val (year, month) = yearMonth
        val title = "Tháng ${month + 1}, $year"

        val days = generateMonthDays(year, month, allEvs)

        CalendarUiState(
            selectedDate = date,
            viewMode = mode,
            monthTitle = title,
            monthDays = days,
            eventsForSelectedDate = dayEvents,
            allEvents = allEvs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(selectedDate = sdf.format(Date()))
    )

    private fun generateMonthDays(year: Int, month: Int, allEvs: List<EventEntity>): List<MonthDay> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val todayStr = sdf.format(Date())

        // Monday = 1, Sunday = 7
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Convert Java Calendar (Sunday=1, Monday=2) to Monday-start (Monday=0 ... Sunday=6)
        val leadingDays = (firstDayOfWeek - Calendar.MONDAY + 7) % 7

        val list = mutableListOf<MonthDay>()

        // 1. Previous month trailing days
        if (leadingDays > 0) {
            val prevCal = cal.clone() as Calendar
            prevCal.add(Calendar.DAY_OF_MONTH, -leadingDays)
            for (i in 0 until leadingDays) {
                val dStr = sdf.format(prevCal.time)
                list.add(
                    MonthDay(
                        dateString = dStr,
                        dayNumber = prevCal.get(Calendar.DAY_OF_MONTH),
                        isCurrentMonth = false,
                        isToday = dStr == todayStr,
                        eventCount = allEvs.count { it.dateStart == dStr || (it.dateStart <= dStr && it.dateEnd >= dStr) }
                    )
                )
                prevCal.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // 2. Current month days
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, i)
            val dStr = sdf.format(cal.time)
            list.add(
                MonthDay(
                    dateString = dStr,
                    dayNumber = i,
                    isCurrentMonth = true,
                    isToday = dStr == todayStr,
                    eventCount = allEvs.count { it.dateStart == dStr || (it.dateStart <= dStr && it.dateEnd >= dStr) }
                )
            )
        }

        // 3. Next month leading days to complete full rows (multiple of 7)
        val remaining = (7 - (list.size % 7)) % 7
        if (remaining > 0) {
            val nextCal = cal.clone() as Calendar
            nextCal.set(Calendar.DAY_OF_MONTH, maxDays)
            for (i in 1..remaining) {
                nextCal.add(Calendar.DAY_OF_MONTH, 1)
                val dStr = sdf.format(nextCal.time)
                list.add(
                    MonthDay(
                        dateString = dStr,
                        dayNumber = nextCal.get(Calendar.DAY_OF_MONTH),
                        isCurrentMonth = false,
                        isToday = dStr == todayStr,
                        eventCount = allEvs.count { it.dateStart == dStr || (it.dateStart <= dStr && it.dateEnd >= dStr) }
                    )
                )
            }
        }

        return list
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun setViewMode(mode: String) {
        _viewMode.value = mode
    }

    fun previousMonth() {
        val (year, month) = _currentYearMonth.value
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.add(Calendar.MONTH, -1)
        _currentYearMonth.value = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun nextMonth() {
        val (year, month) = _currentYearMonth.value
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.add(Calendar.MONTH, 1)
        _currentYearMonth.value = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    fun goToToday() {
        val now = Calendar.getInstance()
        _currentYearMonth.value = Pair(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
        _selectedDate.value = sdf.format(now.time)
    }

    fun addEvent(
        title: String,
        dateStart: String,
        dateEnd: String = dateStart,
        timeStart: String = "",
        timeEnd: String = "",
        type: String = "work",
        desc: String = ""
    ) {
        viewModelScope.launch {
            repository.insertEvent(
                title = title,
                dateStart = dateStart,
                dateEnd = dateEnd.ifBlank { dateStart },
                timeStart = timeStart,
                timeEnd = timeEnd,
                type = type,
                desc = desc
            )
        }
    }

    fun updateEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            repository.deleteEvent(id)
        }
    }
}
