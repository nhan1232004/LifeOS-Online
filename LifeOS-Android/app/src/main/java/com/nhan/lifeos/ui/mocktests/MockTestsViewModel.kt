package com.nhan.lifeos.ui.mocktests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.MockTestEntity
import com.nhan.lifeos.data.repository.MockTestRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MockTestsUiState(
    val tests: List<MockTestEntity> = emptyList(),
    val totalCount: Int = 0,
    val avgScore: Double = 0.0,
    val avgList: Double = 0.0,
    val avgRead: Double = 0.0,
    val avgSpeak: Double = 0.0,
    val avgWrite: Double = 0.0
)

class MockTestsViewModel(private val repository: MockTestRepository) : ViewModel() {

    val uiState: StateFlow<MockTestsUiState> = repository.allMockTests.map { list ->
        val count = list.size
        if (count == 0) {
            MockTestsUiState()
        } else {
            val totalList = list.sumOf { it.list } / count
            val totalRead = list.sumOf { it.read } / count
            val totalSpeak = list.sumOf { it.speak } / count
            val totalWrite = list.sumOf { it.write } / count
            val totalAvg = list.sumOf { it.total } / count
            MockTestsUiState(
                tests = list,
                totalCount = count,
                avgScore = Math.round(totalAvg * 10.0) / 10.0,
                avgList = Math.round(totalList * 10.0) / 10.0,
                avgRead = Math.round(totalRead * 10.0) / 10.0,
                avgSpeak = Math.round(totalSpeak * 10.0) / 10.0,
                avgWrite = Math.round(totalWrite * 10.0) / 10.0
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MockTestsUiState()
    )

    fun addMockTest(
        name: String,
        date: String,
        list: Double,
        read: Double,
        speak: Double,
        write: Double,
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.insertMockTest(name, date, list, read, speak, write, note)
        }
    }

    fun deleteMockTest(id: String) {
        viewModelScope.launch {
            repository.deleteMockTest(id)
        }
    }
}
