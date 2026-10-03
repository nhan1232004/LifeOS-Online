package com.nhan.lifeos.data.repository

import com.nhan.lifeos.data.local.LifeOSDatabase
import com.nhan.lifeos.data.local.entity.EventEntity
import com.nhan.lifeos.data.local.entity.GoalEntity
import com.nhan.lifeos.data.local.entity.HabitEntity
import com.nhan.lifeos.data.local.entity.JournalEntity
import com.nhan.lifeos.data.local.entity.MockTestEntity
import com.nhan.lifeos.data.local.entity.NoteEntity
import com.nhan.lifeos.data.local.entity.ProjectEntity
import com.nhan.lifeos.data.local.entity.ProjectTaskEntity
import com.nhan.lifeos.data.local.entity.TodoEntity
import com.nhan.lifeos.data.local.entity.TransactionEntity
import com.nhan.lifeos.data.local.entity.VocabEntity
import com.nhan.lifeos.data.network.FirestoreSyncService
import com.nhan.lifeos.data.preferences.UserPreferencesRepository
import com.nhan.lifeos.data.preferences.UserSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Success(val totalSynced: Int, val timestamp: Long = System.currentTimeMillis()) : SyncState()
    data class Error(val message: String) : SyncState()
}

class CloudSyncRepository(
    private val database: LifeOSDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val syncService: FirestoreSyncService = FirestoreSyncService()
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    suspend fun syncAll(session: UserSession): Result<Int> = withContext(Dispatchers.IO) {
        if (!session.canSyncOnline) {
            return@withContext Result.failure(Exception("Đang ở chế độ Khách hoặc chưa đăng nhập tài khoản."))
        }

        _syncState.value = SyncState.Syncing
        val uid = session.userId
        val token = session.idToken
        val refresh = session.refreshToken

        val onRefreshed: suspend (String, String) -> Unit = { newToken, newRefresh ->
            preferencesRepository.updateTokens(newToken, newRefresh)
        }

        try {
            var syncedCount = 0

            // 1. Sync Todos
            syncedCount += syncTodos(uid, token, refresh, onRefreshed)

            // 2. Sync Events
            syncedCount += syncEvents(uid, token, refresh, onRefreshed)

            // 3. Sync Projects
            syncedCount += syncProjects(uid, token, refresh, onRefreshed)

            // 4. Sync Finance (Income & Expense)
            syncedCount += syncFinance(uid, token, refresh, onRefreshed)

            // 5. Sync Notes
            syncedCount += syncNotes(uid, token, refresh, onRefreshed)

            // 6. Sync Habits
            syncedCount += syncHabits(uid, token, refresh, onRefreshed)

            // 7. Sync Goals
            syncedCount += syncGoals(uid, token, refresh, onRefreshed)

            // 8. Sync Journal
            syncedCount += syncJournal(uid, token, refresh, onRefreshed)

            // 9. Sync Vocab
            syncedCount += syncVocab(uid, token, refresh, onRefreshed)

            // 10. Sync Proj Tasks
            syncedCount += syncProjTasks(uid, token, refresh, onRefreshed)

            // 11. Sync Mock Tests
            syncedCount += syncMockTests(uid, token, refresh, onRefreshed)

            val now = System.currentTimeMillis()
            preferencesRepository.updateLastSyncedAt(now)
            _syncState.value = SyncState.Success(syncedCount, now)
            Result.success(syncedCount)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "Lỗi kết nối khi đồng bộ dữ liệu"
            _syncState.value = SyncState.Error(msg)
            Result.failure(e)
        }
    }

    /**
     * Tự động đẩy dữ liệu lên Firebase Firestore trong nền ngay khi người dùng thêm/sửa/xóa trên Android.
     * Hoàn toàn không chặn luồng giao diện người dùng (Non-blocking).
     */
    fun triggerAutoSync(collectionName: String) {
        syncScope.launch {
            try {
                val session = preferencesRepository.userSessionFlow.first()
                if (!session.canSyncOnline) return@launch
                val uid = session.userId
                val token = session.idToken
                val refresh = session.refreshToken
                val onRefreshed: suspend (String, String) -> Unit = { newToken, newRefresh ->
                    preferencesRepository.updateTokens(newToken, newRefresh)
                }

                when (collectionName) {
                    "todos" -> syncTodos(uid, token, refresh, onRefreshed)
                    "events" -> syncEvents(uid, token, refresh, onRefreshed)
                    "projects" -> {
                        syncProjects(uid, token, refresh, onRefreshed)
                        syncProjTasks(uid, token, refresh, onRefreshed)
                    }
                    "proj_tasks" -> syncProjTasks(uid, token, refresh, onRefreshed)
                    "finance", "income", "expense" -> syncFinance(uid, token, refresh, onRefreshed)
                    "notes" -> syncNotes(uid, token, refresh, onRefreshed)
                    "habits" -> syncHabits(uid, token, refresh, onRefreshed)
                    "goals" -> syncGoals(uid, token, refresh, onRefreshed)
                    "journal" -> syncJournal(uid, token, refresh, onRefreshed)
                    "vocab" -> syncVocab(uid, token, refresh, onRefreshed)
                    "mocktests" -> syncMockTests(uid, token, refresh, onRefreshed)
                    else -> syncAll(session)
                }
                preferencesRepository.updateLastSyncedAt(System.currentTimeMillis())
            } catch (e: Exception) {
                // Background auto-sync safely fails if offline
            }
        }
    }

    // ─── 1. Todos ─────────────────────────────────────────────────────────────
    private suspend fun syncTodos(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("todos", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())

        val localItems = database.todoDao().getAllTodos().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        // Merge remote into local
        remoteItems.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val todo = TodoEntity(
                    id = id,
                    text = json.optString("text"),
                    done = json.optBoolean("done", false),
                    priority = json.optString("priority", "mid"),
                    date = json.optString("date"),
                    note = json.optString("note"),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || todo.updatedAt >= existing.updatedAt) {
                    database.todoDao().insertTodo(todo)
                    localMap[id] = todo
                }
            }
        }

        // Push unified state to cloud
        val pushList = localMap.values.map { todo ->
            JSONObject().apply {
                put("id", todo.id)
                put("text", todo.text)
                put("done", todo.done)
                put("priority", todo.priority)
                put("date", todo.date)
                put("note", todo.note)
                put("updatedAt", todo.updatedAt)
            }
        }
        syncService.setCollectionItems("todos", uid, pushList, token, refresh, onRefreshed)
        return localMap.size
    }

    // ─── 2. Events ────────────────────────────────────────────────────────────
    private suspend fun syncEvents(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("events", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())

        val localItems = database.eventDao().getAllEvents().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        remoteItems.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val event = EventEntity(
                    id = id,
                    title = json.optString("title"),
                    dateStart = json.optString("dateStart", json.optString("date")),
                    dateEnd = json.optString("dateEnd", json.optString("date")),
                    timeStart = json.optString("timeStart"),
                    timeEnd = json.optString("timeEnd"),
                    type = json.optString("type", "work"),
                    desc = json.optString("desc"),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || event.updatedAt >= existing.updatedAt) {
                    database.eventDao().insertEvent(event)
                    localMap[id] = event
                }
            }
        }

        val pushList = localMap.values.map { ev ->
            JSONObject().apply {
                put("id", ev.id)
                put("title", ev.title)
                put("dateStart", ev.dateStart)
                put("dateEnd", ev.dateEnd)
                put("timeStart", ev.timeStart)
                put("timeEnd", ev.timeEnd)
                put("type", ev.type)
                put("desc", ev.desc)
                put("updatedAt", ev.updatedAt)
            }
        }
        syncService.setCollectionItems("events", uid, pushList, token, refresh, onRefreshed)
        return localMap.size
    }

    // ─── 3. Projects ──────────────────────────────────────────────────────────
    private suspend fun syncProjects(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("projects", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())

        val localItems = database.projectDao().getAllProjects().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        remoteItems.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val tagsList = mutableListOf<String>()
                val tagsArr = json.optJSONArray("tags")
                if (tagsArr != null) {
                    for (i in 0 until tagsArr.length()) tagsList.add(tagsArr.optString(i))
                }
                val proj = ProjectEntity(
                    id = id,
                    name = json.optString("name"),
                    status = json.optString("status", "Cần làm"),
                    priority = json.optString("priority", "mid"),
                    budget = json.optLong("budget", 0L),
                    due = json.optString("due"),
                    desc = json.optString("desc"),
                    tags = tagsList,
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || proj.updatedAt >= existing.updatedAt) {
                    database.projectDao().insertProject(proj)
                    localMap[id] = proj
                }
            }
        }

        val pushList = localMap.values.map { p ->
            JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("status", p.status)
                put("priority", p.priority)
                put("budget", p.budget)
                put("due", p.due)
                put("desc", p.desc)
                put("tags", JSONArray(p.tags))
                put("updatedAt", p.updatedAt)
            }
        }
        syncService.setCollectionItems("projects", uid, pushList, token, refresh, onRefreshed)
        return localMap.size
    }

    // ─── 4. Finance (Income & Expense) ────────────────────────────────────────
    private suspend fun syncFinance(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteInc = syncService.getCollectionItems("income", uid, token, refresh, onRefreshed).getOrDefault(emptyList())
        val remoteExp = syncService.getCollectionItems("expense", uid, token, refresh, onRefreshed).getOrDefault(emptyList())

        val localItems = database.transactionDao().getAllTransactions().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        remoteInc.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val tx = TransactionEntity(
                    id = id,
                    type = "income",
                    categoryOrSource = json.optString("src", "Thu nhập khác"),
                    amount = json.optLong("amt", 0L),
                    date = json.optString("date"),
                    paymentMethod = json.optString("pay", ""),
                    note = json.optString("note", ""),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || tx.updatedAt >= existing.updatedAt) {
                    database.transactionDao().insertTransaction(tx)
                    localMap[id] = tx
                }
            }
        }

        remoteExp.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val tx = TransactionEntity(
                    id = id,
                    type = "expense",
                    categoryOrSource = json.optString("cat", "Chi tiêu khác"),
                    amount = json.optLong("amt", 0L),
                    date = json.optString("date"),
                    paymentMethod = json.optString("pay", "Tiền mặt"),
                    note = json.optString("note", ""),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || tx.updatedAt >= existing.updatedAt) {
                    database.transactionDao().insertTransaction(tx)
                    localMap[id] = tx
                }
            }
        }

        // Push back income list
        val incomeList = localMap.values.filter { it.type == "income" }.map { tx ->
            JSONObject().apply {
                put("id", tx.id)
                put("src", tx.categoryOrSource)
                put("amt", tx.amount)
                put("date", tx.date)
                put("note", tx.note)
                put("updatedAt", tx.updatedAt)
            }
        }
        syncService.setCollectionItems("income", uid, incomeList, token, refresh, onRefreshed)

        // Push back expense list
        val expenseList = localMap.values.filter { it.type == "expense" }.map { tx ->
            JSONObject().apply {
                put("id", tx.id)
                put("cat", tx.categoryOrSource)
                put("amt", tx.amount)
                put("date", tx.date)
                put("pay", tx.paymentMethod)
                put("note", tx.note)
                put("updatedAt", tx.updatedAt)
            }
        }
        syncService.setCollectionItems("expense", uid, expenseList, token, refresh, onRefreshed)

        return localMap.size
    }

    // ─── 5. Notes ─────────────────────────────────────────────────────────────
    private suspend fun syncNotes(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("notes", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())

        val localItems = database.noteDao().getAllNotes().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        remoteItems.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val tagsList = mutableListOf<String>()
                val tagsArr = json.optJSONArray("tags")
                if (tagsArr != null) {
                    for (i in 0 until tagsArr.length()) tagsList.add(tagsArr.optString(i))
                }
                val note = NoteEntity(
                    id = id,
                    title = json.optString("title"),
                    content = json.optString("content"),
                    tags = tagsList,
                    pinned = json.optBoolean("pinned", false),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || note.updatedAt >= existing.updatedAt) {
                    database.noteDao().insertNote(note)
                    localMap[id] = note
                }
            }
        }

        val pushList = localMap.values.map { n ->
            JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("tags", JSONArray(n.tags))
                put("pinned", n.pinned)
                put("updatedAt", n.updatedAt)
            }
        }
        syncService.setCollectionItems("notes", uid, pushList, token, refresh, onRefreshed)
        return localMap.size
    }

    // ─── 6. Habits ────────────────────────────────────────────────────────────
    private suspend fun syncHabits(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("habits", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())

        val localItems = database.habitDao().getAllHabits().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        remoteItems.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val logObj = json.optJSONObject("log") ?: json.optJSONObject("logsJson") ?: JSONObject()
                val habit = HabitEntity(
                    id = id,
                    name = json.optString("name"),
                    streak = json.optInt("streak", 0),
                    target = json.optString("target", "Mỗi ngày"),
                    logsJson = logObj.toString(),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || habit.updatedAt >= existing.updatedAt) {
                    database.habitDao().insertHabit(habit)
                    localMap[id] = habit
                }
            }
        }

        val pushList = localMap.values.map { h ->
            val logObj = try { JSONObject(h.logsJson) } catch (e: Exception) { JSONObject() }
            JSONObject().apply {
                put("id", h.id)
                put("name", h.name)
                put("streak", h.streak)
                put("target", h.target)
                put("log", logObj)
                put("updatedAt", h.updatedAt)
            }
        }
        syncService.setCollectionItems("habits", uid, pushList, token, refresh, onRefreshed)
        return localMap.size
    }

    // ─── 7. Goals ─────────────────────────────────────────────────────────────
    private suspend fun syncGoals(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("goals", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())

        val localItems = database.goalDao().getAllGoals().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        remoteItems.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val goal = GoalEntity(
                    id = id,
                    title = json.optString("name", json.optString("title")),
                    category = json.optString("category", "Tài chính"),
                    targetValue = json.optLong("target", json.optLong("targetValue", 0L)),
                    currentValue = json.optLong("saved", json.optLong("currentValue", 0L)),
                    unit = json.optString("unit", "₫"),
                    deadline = json.optString("due", json.optString("deadline")),
                    note = json.optString("note", ""),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || goal.updatedAt >= existing.updatedAt) {
                    database.goalDao().insertGoal(goal)
                    localMap[id] = goal
                }
            }
        }

        val pushList = localMap.values.map { g ->
            JSONObject().apply {
                put("id", g.id)
                put("name", g.title)
                put("category", g.category)
                put("target", g.targetValue)
                put("saved", g.currentValue)
                put("due", g.deadline)
                put("unit", g.unit)
                put("note", g.note)
                put("updatedAt", g.updatedAt)
            }
        }
        syncService.setCollectionItems("goals", uid, pushList, token, refresh, onRefreshed)
        return localMap.size
    }

    // ─── 8. Journal ───────────────────────────────────────────────────────────
    private suspend fun syncJournal(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("journal", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())

        val localItems = database.journalDao().getAllEntries().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        remoteItems.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val entry = JournalEntity(
                    id = id,
                    date = json.optString("date"),
                    mood = json.optString("mood", "😊"),
                    body = json.optString("body"),
                    title = json.optString("title"),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || entry.updatedAt >= existing.updatedAt) {
                    database.journalDao().insertEntry(entry)
                    localMap[id] = entry
                }
            }
        }

        val pushList = localMap.values.map { j ->
            JSONObject().apply {
                put("id", j.id)
                put("date", j.date)
                put("mood", j.mood)
                put("body", j.body)
                put("title", j.title)
                put("updatedAt", j.updatedAt)
            }
        }
        syncService.setCollectionItems("journal", uid, pushList, token, refresh, onRefreshed)
        return localMap.size
    }

    // ─── 9. Vocab ─────────────────────────────────────────────────────────────
    private suspend fun syncVocab(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("vocab", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())

        val localItems = database.vocabDao().getAllVocab().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        remoteItems.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val vocab = VocabEntity(
                    id = id,
                    word = json.optString("word"),
                    pron = json.optString("pron"),
                    type = json.optString("type", "n"),
                    mean = json.optString("mean"),
                    example = json.optString("ex", json.optString("example")),
                    srsLevel = json.optInt("srsLevel", 0),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || vocab.updatedAt >= existing.updatedAt) {
                    database.vocabDao().insertVocab(vocab)
                    localMap[id] = vocab
                }
            }
        }

        val pushList = localMap.values.map { v ->
            JSONObject().apply {
                put("id", v.id)
                put("word", v.word)
                put("pron", v.pron)
                put("type", v.type)
                put("mean", v.mean)
                put("ex", v.example)
                put("srsLevel", v.srsLevel)
                put("updatedAt", v.updatedAt)
            }
        }
        syncService.setCollectionItems("vocab", uid, pushList, token, refresh, onRefreshed)
        return localMap.size
    }

    // ─── 10. Proj Tasks ───────────────────────────────────────────────────────
    private suspend fun syncProjTasks(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("proj_tasks", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())

        val localItems = database.projectTaskDao().getAllTasks().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        remoteItems.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val task = ProjectTaskEntity(
                    id = id,
                    projId = json.optString("projId"),
                    text = json.optString("text"),
                    status = json.optString("status", "Cần làm"),
                    priority = json.optString("priority", "mid"),
                    desc = json.optString("desc"),
                    start = json.optString("start"),
                    due = json.optString("due"),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || task.updatedAt >= existing.updatedAt) {
                    database.projectTaskDao().insertTask(task)
                    localMap[id] = task
                }
            }
        }

        val pushList = localMap.values.map { t ->
            JSONObject().apply {
                put("id", t.id)
                put("projId", t.projId)
                put("text", t.text)
                put("status", t.status)
                put("priority", t.priority)
                put("desc", t.desc)
                put("start", t.start)
                put("due", t.due)
                put("updatedAt", t.updatedAt)
            }
        }
        syncService.setCollectionItems("proj_tasks", uid, pushList, token, refresh, onRefreshed)
        return localMap.size
    }

    // ─── 11. Mock Tests ───────────────────────────────────────────────────────
    private suspend fun syncMockTests(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("mocktests", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())

        val localItems = database.mockTestDao().getAllMockTests().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        remoteItems.forEach { json ->
            val id = json.optString("id")
            if (id.isNotBlank()) {
                val existing = localMap[id]
                val test = MockTestEntity(
                    id = id,
                    name = json.optString("name"),
                    date = json.optString("date"),
                    list = json.optDouble("list", 0.0),
                    read = json.optDouble("read", 0.0),
                    speak = json.optDouble("speak", 0.0),
                    write = json.optDouble("write", 0.0),
                    total = json.optDouble("total", 0.0),
                    note = json.optString("note"),
                    updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                    isSynced = true
                )
                if (existing == null || test.updatedAt >= existing.updatedAt) {
                    database.mockTestDao().insertMockTest(test)
                    localMap[id] = test
                }
            }
        }

        val pushList = localMap.values.map { m ->
            JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("date", m.date)
                put("list", m.list)
                put("read", m.read)
                put("speak", m.speak)
                put("write", m.write)
                put("total", m.total)
                put("note", m.note)
                put("updatedAt", m.updatedAt)
            }
        }
        syncService.setCollectionItems("mocktests", uid, pushList, token, refresh, onRefreshed)
        return localMap.size
    }
}
