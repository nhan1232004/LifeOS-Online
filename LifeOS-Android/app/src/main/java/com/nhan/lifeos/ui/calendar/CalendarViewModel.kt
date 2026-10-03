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
import java.util.Date
import java.util.Locale

data class CalendarUiState(
    val selectedDate: String,
    val eventsForSelectedDate: List<EventEntity> = emptyList(),
    val allEvents: List<EventEntity> = emptyList()
)

class CalendarViewModel(private val repository: TaskTimeRepository) : ViewModel() {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val _selectedDate = MutableStateFlow(sdf.format(Date()))

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedDate,
        _selectedDate.flatMapLatest { date -> repository.getEventsForDate(date) },
        repository.allEvents
    ) { date, dayEvents, allEvs ->
        CalendarUiState(
            selectedDate = date,
            eventsForSelectedDate = dayEvents,
            allEvents = allEvs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState(selectedDate = sdf.format(Date()))
    )

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun addEvent(
        title: String,
        dateStart: String,
        timeStart: String = "",
        timeEnd: String = "",
        type: String = "work",
        desc: String = ""
    ) {
        viewModelScope.launch {
            repository.insertEvent(
                title = title,
                dateStart = dateStart,
                dateEnd = dateStart,
                timeStart = timeStart,
                timeEnd = timeEnd,
                type = type,
                desc = desc
            )
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            repository.deleteEvent(id)
        }
    }
}
