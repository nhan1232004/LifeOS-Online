package com.nhan.lifeos.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.JournalEntity
import com.nhan.lifeos.data.repository.PersonalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class JournalUiState(
    val entries: List<JournalEntity> = emptyList(),
    val isLoading: Boolean = false
)

class JournalViewModel(private val repository: PersonalRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(JournalUiState(isLoading = true))
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            repository.allJournalEntries.collect { list ->
                _uiState.value = JournalUiState(
                    entries = list,
                    isLoading = false
                )
            }
        }
    }

    fun addEntry(date: String, mood: String, body: String, title: String = "") {
        viewModelScope.launch {
            repository.insertJournal(date, mood, body, title)
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            repository.deleteJournal(id)
        }
    }
}
