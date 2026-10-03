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
        journalDao.deleteById(id)
        cloudSyncRepo?.triggerAutoSync("journal")
    }

    // ─── Demo Seeding ─────────────────────────────────────────────────────────
    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val currentNotes = noteDao.getAllNotes().first()
        if (currentNotes.isEmpty()) {
            val sampleNotes = listOf(
                NoteEntity(
                    id = "note-1",
                    title = "Kiến trúc Clean Architecture trong Jetpack Compose",
                    content = "1. Domain layer độc lập UI & Data.\n2. StateFlow & immutable UI state.\n3. Room Database làm single source of truth.\n4. Coroutines Dispatchers.IO cho I/O tasks.",
                    tags = listOf("Android", "Architecture", "Kotlin"),
                    pinned = true
                ),
                NoteEntity(
                    id = "note-2",
                    title = "Ý tưởng phát triển LifeOS Mobile",
                    content = "Phát triển phiên bản 100% Native bằng Android Studio giúp tối ưu bộ nhớ, mượt mà hơn 500% so với bản web cũ và không bao giờ gặp lỗi browser auth session.",
                    tags = listOf("LifeOS", "Project"),
                    pinned = true
                ),
                NoteEntity(
                    id = "note-3",
                    title = "Checklist Chuẩn bị Họp Sprint",
                    content = "- Xem lại Jira backlog.\n- Chuẩn bị demo bản build APK mới.\n- Ghi chú các điểm nghẽn và cải tiến tuần tới.",
                    tags = listOf("Work"),
                    pinned = false
                )
            )
            noteDao.insertAll(sampleNotes)
        }

        val currentHabits = habitDao.getAllHabits().first()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        if (currentHabits.isEmpty()) {
            val cal = java.util.Calendar.getInstance()
            val logs = JSONObject()
            for (i in 0..4) {
                logs.put(sdf.format(cal.time), true)
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            }

            val sampleHabits = listOf(
                HabitEntity(
                    id = "habit-1",
                    name = "Uống 2L nước mỗi ngày",
                    streak = 5,
                    target = "Mỗi ngày",
                    logsJson = logs.toString()
                ),
                HabitEntity(
                    id = "habit-2",
                    name = "Đọc sách 30 phút",
                    streak = 4,
                    target = "Mỗi ngày",
                    logsJson = logs.toString()
                ),
                HabitEntity(
                    id = "habit-3",
                    name = "Tập thể dục / Gym",
                    streak = 3,
                    target = "5 ngày/tuần",
                    logsJson = logs.toString()
                ),
                HabitEntity(
                    id = "habit-4",
                    name = "Đi ngủ trước 23:00",
                    streak = 2,
                    target = "Mỗi ngày",
                    logsJson = logs.toString()
                )
            )
            habitDao.insertAll(sampleHabits)
        }

        val currentJournal = journalDao.getAllEntries().first()
        if (currentJournal.isEmpty()) {
            val sampleJournal = listOf(
                JournalEntity(
                    id = "jr-1",
                    date = todayStr,
                    mood = "🔥",
                    title = "Một ngày làm việc hiệu suất cao",
                    body = "Hôm nay hoàn thành xuất sắc các phase chuyển đổi LifeOS sang native Android Kotlin! Ứng dụng chạy mượt mà, giao diện glassmorphism dark theme rất đẹp mắt."
                ),
                JournalEntity(
                    id = "jr-2",
                    date = todayStr,
                    mood = "💡",
                    title = "Ý tưởng tối ưu Offline-First",
                    body = "Sử dụng Room DB làm local single source of truth giúp app chạy cực nhanh ngay cả khi bật chế độ máy bay. Người dùng có thể hoàn toàn yên tâm về dữ liệu."
                )
            )
            journalDao.insertAll(sampleJournal)
        }
    }
}
