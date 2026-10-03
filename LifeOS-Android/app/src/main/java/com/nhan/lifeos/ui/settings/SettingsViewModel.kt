package com.nhan.lifeos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.LifeOSDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class DatabaseStats(
    val todosCount: Int = 0,
    val eventsCount: Int = 0,
    val projectsCount: Int = 0,
    val transactionsCount: Int = 0,
    val goalsCount: Int = 0,
    val notesCount: Int = 0,
    val habitsCount: Int = 0,
    val journalCount: Int = 0,
    val vocabCount: Int = 0
)

data class SettingsUiState(
    val stats: DatabaseStats = DatabaseStats(),
    val appVersion: String = "1.0.0",
    val isExporting: Boolean = false
)

class SettingsViewModel(private val database: LifeOSDatabase) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            val todos = database.todoDao().getAllTodos().first().size
            val events = database.eventDao().getAllEvents().first().size
            val projects = database.projectDao().getAllProjects().first().size
            val txs = database.transactionDao().getAllTransactions().first().size
            val goals = database.goalDao().getAllGoals().first().size
            val notes = database.noteDao().getAllNotes().first().size
            val habits = database.habitDao().getAllHabits().first().size
            val journal = database.journalDao().getAllEntries().first().size
            val vocab = database.vocabDao().getAllVocab().first().size

            _uiState.value = _uiState.value.copy(
                stats = DatabaseStats(
                    todosCount = todos,
                    eventsCount = events,
                    projectsCount = projects,
                    transactionsCount = txs,
                    goalsCount = goals,
                    notesCount = notes,
                    habitsCount = habits,
                    journalCount = journal,
                    vocabCount = vocab
                )
            )
        }
    }

    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "LifeOS Native Android")
        root.put("version", "1.0.0")
        root.put("exportedAt", System.currentTimeMillis())

        val todos = database.todoDao().getAllTodos().first()
        val todosArray = JSONArray()
        todos.forEach {
            todosArray.put(JSONObject().apply {
                put("id", it.id)
                put("text", it.text)
                put("done", it.done)
                put("priority", it.priority)
                put("date", it.date)
                put("note", it.note)
            })
        }
        root.put("todos", todosArray)

        val txs = database.transactionDao().getAllTransactions().first()
        val txsArray = JSONArray()
        txs.forEach {
            txsArray.put(JSONObject().apply {
                put("id", it.id)
                put("type", it.type)
                put("categoryOrSource", it.categoryOrSource)
                put("amount", it.amount)
                put("date", it.date)
                put("paymentMethod", it.paymentMethod)
                put("note", it.note)
            })
        }
        root.put("transactions", txsArray)

        val notes = database.noteDao().getAllNotes().first()
        val notesArray = JSONArray()
        notes.forEach {
            notesArray.put(JSONObject().apply {
                put("id", it.id)
                put("title", it.title)
                put("content", it.content)
                put("pinned", it.pinned)
            })
        }
        root.put("notes", notesArray)

        root.toString(2)
    }

    fun clearAllData(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            withContext(Dispatchers.Main) {
                loadStats()
                onComplete()
            }
        }
    }
}
