package com.nhan.lifeos.data.repository

import com.nhan.lifeos.data.local.LifeOSDatabase
import com.nhan.lifeos.data.local.entity.HabitEntity
import com.nhan.lifeos.data.local.entity.JournalEntity
import com.nhan.lifeos.data.local.entity.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PersonalRepository(
    private val database: LifeOSDatabase,
    private val cloudSyncRepo: CloudSyncRepository? = null
) {
    private val noteDao = database.noteDao()
    private val habitDao = database.habitDao()
    private val journalDao = database.journalDao()

    // ─── Notes ────────────────────────────────────────────────────────────────
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun insertNote(title: String, content: String, tags: List<String>, pinned: Boolean) = withContext(Dispatchers.IO) {
        val note = NoteEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            content = content,
            tags = tags,
            pinned = pinned,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insertNote(note)
        cloudSyncRepo?.triggerAutoSync("notes")
    }

    suspend fun updateNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
        cloudSyncRepo?.triggerAutoSync("notes")
    }

    suspend fun togglePinNote(id: String) = withContext(Dispatchers.IO) {
        val list = noteDao.getAllNotes().first()
        val note = list.find { it.id == id } ?: return@withContext
        noteDao.updateNote(note.copy(pinned = !note.pinned, updatedAt = System.currentTimeMillis()))
        cloudSyncRepo?.triggerAutoSync("notes")
    }

    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        cloudSyncRepo?.recordDeletedItem(id, "notes")
        noteDao.deleteById(id)
        cloudSyncRepo?.triggerAutoSync("notes")
    }

    // ─── Habits ───────────────────────────────────────────────────────────────
    val allHabits: Flow<List<HabitEntity>> = habitDao.getAllHabits()

    suspend fun insertHabit(name: String, target: String = "Mỗi ngày") = withContext(Dispatchers.IO) {
        val habit = HabitEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            streak = 0,
            target = target,
            logsJson = "{}",
            updatedAt = System.currentTimeMillis()
        )
        habitDao.insertHabit(habit)
        cloudSyncRepo?.triggerAutoSync("habits")
    }

    suspend fun toggleHabitDay(id: String, date: String) = withContext(Dispatchers.IO) {
        val list = habitDao.getAllHabits().first()
        val habit = list.find { it.id == id } ?: return@withContext

        val json = try { JSONObject(habit.logsJson) } catch (e: Exception) { JSONObject() }
        val isDone = json.optBoolean(date, false)
        if (isDone) {
            json.remove(date)
        } else {
            json.put(date, true)
        }

        // Calculate simple streak
        var streakCount = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = java.util.Calendar.getInstance()
        for (i in 0..60) {
            val dStr = sdf.format(cal.time)
            if (json.optBoolean(dStr, false)) {
                streakCount++
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                if (i == 0) {
                    // Check if yesterday had it
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    continue
                }
                break
            }
        }

        habitDao.updateHabit(
            habit.copy(
                logsJson = json.toString(),
                streak = streakCount,
                updatedAt = System.currentTimeMillis()
            )
        )
        cloudSyncRepo?.triggerAutoSync("habits")
    }

    suspend fun deleteHabit(id: String) = withContext(Dispatchers.IO) {
        cloudSyncRepo?.recordDeletedItem(id, "habits")
        habitDao.deleteById(id)
        cloudSyncRepo?.triggerAutoSync("habits")
    }

    // ─── Journal ──────────────────────────────────────────────────────────────
    val allJournalEntries: Flow<List<JournalEntity>> = journalDao.getAllEntries()

    suspend fun insertJournal(date: String, mood: String, body: String, title: String = "") = withContext(Dispatchers.IO) {
        val entry = JournalEntity(
            id = UUID.randomUUID().toString(),
            date = date,
            mood = mood,
            body = body,
            title = title,
            updatedAt = System.currentTimeMillis()
        )
        journalDao.insertEntry(entry)
        cloudSyncRepo?.triggerAutoSync("journal")
    }

    suspend fun deleteJournal(id: String) = withContext(Dispatchers.IO) {
        cloudSyncRepo?.recordDeletedItem(id, "journal")
        journalDao.deleteById(id)
        cloudSyncRepo?.triggerAutoSync("journal")
    }

    // ─── Demo Seeding ─────────────────────────────────────────────────────────
    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        // Sample data disabled per user request: start completely clean
    }
}
