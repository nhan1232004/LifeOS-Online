package com.nhan.lifeos.ui.vocab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.VocabEntity
import com.nhan.lifeos.data.repository.VocabRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VocabUiState(
    val vocabList: List<VocabEntity> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val masteredCount: Int = 0,
    val learningCount: Int = 0,
    val isLoading: Boolean = false
) {
    val currentCard: VocabEntity?
        get() = if (vocabList.isNotEmpty() && currentIndex in vocabList.indices) vocabList[currentIndex] else null
}

class VocabViewModel(private val repository: VocabRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(VocabUiState(isLoading = true))
    val uiState: StateFlow<VocabUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            repository.allVocab.collect { list ->
                val mastered = list.count { it.srsLevel >= 3 }
                val learning = list.size - mastered
                _uiState.value = _uiState.value.copy(
                    vocabList = list,
                    currentIndex = _uiState.value.currentIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0)),
                    masteredCount = mastered,
                    learningCount = learning,
                    isLoading = false
                )
            }
        }
    }

    fun flipCard() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun nextCard() {
        val total = _uiState.value.vocabList.size
        if (total > 0) {
            val nextIdx = (_uiState.value.currentIndex + 1) % total
            _uiState.value = _uiState.value.copy(currentIndex = nextIdx, isFlipped = false)
        }
    }

    fun prevCard() {
        val total = _uiState.value.vocabList.size
        if (total > 0) {
            val prevIdx = if (_uiState.value.currentIndex - 1 < 0) total - 1 else _uiState.value.currentIndex - 1
            _uiState.value = _uiState.value.copy(currentIndex = prevIdx, isFlipped = false)
        }
    }

    fun markRemembered(remembered: Boolean) {
        val card = _uiState.value.currentCard ?: return
        viewModelScope.launch {
            val newLevel = if (remembered) (card.srsLevel + 1).coerceAtMost(3) else 0
            repository.updateSrsLevel(card.id, newLevel)
            nextCard()
        }
    }

    fun addVocab(word: String, pron: String, type: String, mean: String, example: String) {
        viewModelScope.launch {
            repository.insertVocab(word, pron, type, mean, example)
        }
    }

    fun deleteVocab(id: String) {
        viewModelScope.launch {
            repository.deleteVocab(id)
        }
    }
}
