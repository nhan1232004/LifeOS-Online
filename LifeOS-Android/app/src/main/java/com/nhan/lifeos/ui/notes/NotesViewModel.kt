package com.nhan.lifeos.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.NoteEntity
import com.nhan.lifeos.data.repository.PersonalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

class NotesViewModel(private val repository: PersonalRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState(isLoading = true))
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private var allNotes: List<NoteEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.allNotes.collect { list ->
                allNotes = list
                filterNotes()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        filterNotes()
    }

    fun addNote(title: String, content: String, tags: List<String>, pinned: Boolean) {
        viewModelScope.launch {
            repository.insertNote(title, content, tags, pinned)
        }
    }

    fun togglePin(id: String) {
        viewModelScope.launch {
            repository.togglePinNote(id)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    private fun filterNotes() {
        val q = _uiState.value.searchQuery.trim().lowercase()
        val filtered = if (q.isBlank()) {
            allNotes
        } else {
            allNotes.filter {
                it.title.lowercase().contains(q) ||
                it.content.lowercase().contains(q) ||
                it.tags.any { tag -> tag.lowercase().contains(q) }
            }
        }
        _uiState.value = _uiState.value.copy(notes = filtered, isLoading = false)
    }
}
