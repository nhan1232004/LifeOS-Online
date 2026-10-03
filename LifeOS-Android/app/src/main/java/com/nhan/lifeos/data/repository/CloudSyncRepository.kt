package com.nhan.lifeos.data.repository

import com.nhan.lifeos.data.local.LifeOSDatabase
import com.nhan.lifeos.data.local.entity.DeletedItemEntity
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    /**
     * Ghi nhận xóa mục cục bộ (Tombstone) để đảm bảo khi đồng bộ với Cloud,
     * mục này sẽ bị xóa trên Firestore thay vì bị khôi phục lại.
     */
    suspend fun recordDeletedItem(id: String, collectionName: String) {
        try {
            database.deletedItemDao().insert(
                DeletedItemEntity(id = id, collectionName = collectionName, deletedAt = System.currentTimeMillis())
            )
        } catch (_: Exception) {}
    }

    suspend fun syncAll(session: UserSession): Result<Int> = withContext(Dispatchers.IO) {
        if (!session.canSyncOnline) {
            return@withContext Result.failure(Exception("Chưa đăng nhập trực tuyến"))
        }

        _syncState.value = SyncState.Syncing

        try {
            val uid = session.userId
            val token = session.idToken
            val refresh = session.refreshToken
            val onRefreshed: suspend (String, String) -> Unit = { newToken, newRefresh ->
                preferencesRepository.updateTokens(newToken, newRefresh)
            }

            var syncedCount = 0

            coroutineScope {
                val dTodos = async { syncTodos(uid, token, refresh, onRefreshed) }
                val dEvents = async { syncEvents(uid, token, refresh, onRefreshed) }
                val dProjects = async { syncProjects(uid, token, refresh, onRefreshed) }
                val dFinance = async { syncFinance(uid, token, refresh, onRefreshed) }
                val dNotes = async { syncNotes(uid, token, refresh, onRefreshed) }
                val dHabits = async { syncHabits(uid, token, refresh, onRefreshed) }
                val dGoals = async { syncGoals(uid, token, refresh, onRefreshed) }
                val dJournal = async { syncJournal(uid, token, refresh, onRefreshed) }
                val dVocab = async { syncVocab(uid, token, refresh, onRefreshed) }
                val dProjTasks = async { syncProjTasks(uid, token, refresh, onRefreshed) }
                val dMockTests = async { syncMockTests(uid, token, refresh, onRefreshed) }

                syncedCount = dTodos.await() + dEvents.await() + dProjects.await() +
                        dFinance.await() + dNotes.await() + dHabits.await() +
                        dGoals.await() + dJournal.await() + dVocab.await() +
                        dProjTasks.await() + dMockTests.await()
            }

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
            } catch (_: Exception) {
                // Background auto-sync safely fails if offline
            }
        }
    }

    // ─── 1. Todos ─────────────────────────────────────────────────────────────
    private suspend fun syncTodos(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("todos", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())
        val remoteMap = remoteItems.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val localItems = database.todoDao().getAllTodos().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        val deletedMap = database.deletedItemDao().getByCollection("todos").associateBy { it.id }

        // Process remote items
        remoteMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val todo = parseTodoJson(json).copy(isSynced = true)
                    database.todoDao().insertTodo(todo)
                    localMap[id] = todo
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val todo = parseTodoJson(json).copy(isSynced = true)
                    database.todoDao().insertTodo(todo)
                    localMap[id] = todo
                }
            }
        }

        // Process local items missing from remote
        localItems.forEach { local ->
            if (deletedMap.containsKey(local.id)) {
                database.todoDao().deleteById(local.id)
                localMap.remove(local.id)
            } else if (!remoteMap.containsKey(local.id)) {
                if (local.isSynced) {
                    // Deleted on web!
                    database.todoDao().deleteById(local.id)
                    localMap.remove(local.id)
                }
            }
        }

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

        localMap.values.filter { !it.isSynced }.forEach {
            database.todoDao().insertTodo(it.copy(isSynced = true))
        }
        deletedMap.values.forEach {
            if (!remoteMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }

        return localMap.size
    }

    private fun parseTodoJson(json: JSONObject) = TodoEntity(
        id = json.getString("id"),
        text = json.optString("text"),
        done = json.optBoolean("done", false),
        priority = json.optString("priority", "mid"),
        date = json.optString("date"),
        note = json.optString("note"),
        updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
        isSynced = true
    )

    // ─── 2. Events ────────────────────────────────────────────────────────────
    private suspend fun syncEvents(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("events", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())
        val remoteMap = remoteItems.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val localItems = database.eventDao().getAllEvents().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()
        val deletedMap = database.deletedItemDao().getByCollection("events").associateBy { it.id }

        remoteMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val ev = parseEventJson(json).copy(isSynced = true)
                    database.eventDao().insertEvent(ev)
                    localMap[id] = ev
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val ev = parseEventJson(json).copy(isSynced = true)
                    database.eventDao().insertEvent(ev)
                    localMap[id] = ev
                }
            }
        }

        localItems.forEach { local ->
            if (deletedMap.containsKey(local.id)) {
                database.eventDao().deleteById(local.id)
                localMap.remove(local.id)
            } else if (!remoteMap.containsKey(local.id)) {
                if (local.isSynced) {
                    database.eventDao().deleteById(local.id)
                    localMap.remove(local.id)
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

        localMap.values.filter { !it.isSynced }.forEach {
            database.eventDao().insertEvent(it.copy(isSynced = true))
        }
        deletedMap.values.forEach {
            if (!remoteMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }

        return localMap.size
    }

    private fun parseEventJson(json: JSONObject) = EventEntity(
        id = json.getString("id"),
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

    // ─── 3. Projects ──────────────────────────────────────────────────────────
    private suspend fun syncProjects(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("projects", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())
        val remoteMap = remoteItems.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val localItems = database.projectDao().getAllProjects().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()
        val deletedMap = database.deletedItemDao().getByCollection("projects").associateBy { it.id }

        remoteMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val proj = parseProjectJson(json).copy(isSynced = true)
                    database.projectDao().insertProject(proj)
                    localMap[id] = proj
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val proj = parseProjectJson(json).copy(isSynced = true)
                    database.projectDao().insertProject(proj)
                    localMap[id] = proj
                }
            }
        }

        localItems.forEach { local ->
            if (deletedMap.containsKey(local.id)) {
                database.projectDao().deleteById(local.id)
                localMap.remove(local.id)
            } else if (!remoteMap.containsKey(local.id)) {
                if (local.isSynced) {
                    database.projectDao().deleteById(local.id)
                    localMap.remove(local.id)
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
                put("progress", p.progress)
                put("due", p.due)
                put("desc", p.desc)
                put("tags", JSONArray(p.tags))
                put("members", JSONArray(p.members))
                put("updatedAt", p.updatedAt)
            }
        }
        syncService.setCollectionItems("projects", uid, pushList, token, refresh, onRefreshed)

        localMap.values.filter { !it.isSynced }.forEach {
            database.projectDao().insertProject(it.copy(isSynced = true))
        }
        deletedMap.values.forEach {
            if (!remoteMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }

        return localMap.size
    }

    private fun parseProjectJson(json: JSONObject): ProjectEntity {
        val tagsList = mutableListOf<String>()
        val tagsArr = json.optJSONArray("tags")
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) tagsList.add(tagsArr.optString(i))
        }
        val membersList = mutableListOf<String>()
        val membersArr = json.optJSONArray("members")
        if (membersArr != null) {
            for (i in 0 until membersArr.length()) membersList.add(membersArr.optString(i))
        }
        return ProjectEntity(
            id = json.getString("id"),
            name = json.optString("name"),
            status = json.optString("status", "Cần làm"),
            priority = json.optString("priority", "mid"),
            budget = json.optLong("budget", 0L),
            progress = json.optInt("progress", 0),
            due = json.optString("due"),
            desc = json.optString("desc"),
            tags = tagsList,
            members = membersList,
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
            isSynced = true
        )
    }

    // ─── 4. Finance (Income & Expense) ────────────────────────────────────────
    private suspend fun syncFinance(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteInc = syncService.getCollectionItems("income", uid, token, refresh, onRefreshed).getOrDefault(emptyList())
        val remoteIncMap = remoteInc.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val remoteExp = syncService.getCollectionItems("expense", uid, token, refresh, onRefreshed).getOrDefault(emptyList())
        val remoteExpMap = remoteExp.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val localItems = database.transactionDao().getAllTransactions().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()

        val deletedIncMap = database.deletedItemDao().getByCollection("income").associateBy { it.id }
        val deletedExpMap = database.deletedItemDao().getByCollection("expense").associateBy { it.id }

        // Process remote Income
        remoteIncMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedIncMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val tx = parseTxJson(json, "income").copy(isSynced = true)
                    database.transactionDao().insertTransaction(tx)
                    localMap[id] = tx
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val tx = parseTxJson(json, "income").copy(isSynced = true)
                    database.transactionDao().insertTransaction(tx)
                    localMap[id] = tx
                }
            }
        }

        // Process remote Expense
        remoteExpMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedExpMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val tx = parseTxJson(json, "expense").copy(isSynced = true)
                    database.transactionDao().insertTransaction(tx)
                    localMap[id] = tx
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val tx = parseTxJson(json, "expense").copy(isSynced = true)
                    database.transactionDao().insertTransaction(tx)
                    localMap[id] = tx
                }
            }
        }

        // Process local items missing from remote
        localItems.forEach { local ->
            val isInc = local.type == "income"
            val isDeleted = if (isInc) deletedIncMap.containsKey(local.id) else deletedExpMap.containsKey(local.id)
            val existsInRemote = if (isInc) remoteIncMap.containsKey(local.id) else remoteExpMap.containsKey(local.id)

            if (isDeleted) {
                database.transactionDao().deleteById(local.id)
                localMap.remove(local.id)
            } else if (!existsInRemote) {
                if (local.isSynced) {
                    database.transactionDao().deleteById(local.id)
                    localMap.remove(local.id)
                }
            }
        }

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

        localMap.values.filter { !it.isSynced }.forEach {
            database.transactionDao().insertTransaction(it.copy(isSynced = true))
        }

        deletedIncMap.values.forEach {
            if (!remoteIncMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }
        deletedExpMap.values.forEach {
            if (!remoteExpMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }

        return localMap.size
    }

    private fun parseTxJson(json: JSONObject, type: String) = TransactionEntity(
        id = json.getString("id"),
        type = type,
        categoryOrSource = if (type == "income") json.optString("src", "Thu nhập khác") else json.optString("cat", "Chi tiêu khác"),
        amount = json.optLong("amt", 0L),
        date = json.optString("date"),
        paymentMethod = json.optString("pay", if (type == "income") "" else "Tiền mặt"),
        note = json.optString("note", ""),
        updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
        isSynced = true
    )

    // ─── 5. Notes ─────────────────────────────────────────────────────────────
    private suspend fun syncNotes(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("notes", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())
        val remoteMap = remoteItems.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val localItems = database.noteDao().getAllNotes().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()
        val deletedMap = database.deletedItemDao().getByCollection("notes").associateBy { it.id }

        remoteMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val note = parseNoteJson(json).copy(isSynced = true)
                    database.noteDao().insertNote(note)
                    localMap[id] = note
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val note = parseNoteJson(json).copy(isSynced = true)
                    database.noteDao().insertNote(note)
                    localMap[id] = note
                }
            }
        }

        localItems.forEach { local ->
            if (deletedMap.containsKey(local.id)) {
                database.noteDao().deleteById(local.id)
                localMap.remove(local.id)
            } else if (!remoteMap.containsKey(local.id)) {
                if (local.isSynced) {
                    database.noteDao().deleteById(local.id)
                    localMap.remove(local.id)
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

        localMap.values.filter { !it.isSynced }.forEach {
            database.noteDao().insertNote(it.copy(isSynced = true))
        }
        deletedMap.values.forEach {
            if (!remoteMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }

        return localMap.size
    }

    private fun parseNoteJson(json: JSONObject): NoteEntity {
        val tagsList = mutableListOf<String>()
        val tagsArr = json.optJSONArray("tags")
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) tagsList.add(tagsArr.optString(i))
        }
        return NoteEntity(
            id = json.getString("id"),
            title = json.optString("title"),
            content = json.optString("content"),
            tags = tagsList,
            pinned = json.optBoolean("pinned", false),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
            isSynced = true
        )
    }

    // ─── 6. Habits ────────────────────────────────────────────────────────────
    private suspend fun syncHabits(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("habits", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())
        val remoteMap = remoteItems.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val localItems = database.habitDao().getAllHabits().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()
        val deletedMap = database.deletedItemDao().getByCollection("habits").associateBy { it.id }

        remoteMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val habit = parseHabitJson(json).copy(isSynced = true)
                    database.habitDao().insertHabit(habit)
                    localMap[id] = habit
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val habit = parseHabitJson(json).copy(isSynced = true)
                    database.habitDao().insertHabit(habit)
                    localMap[id] = habit
                }
            }
        }

        localItems.forEach { local ->
            if (deletedMap.containsKey(local.id)) {
                database.habitDao().deleteById(local.id)
                localMap.remove(local.id)
            } else if (!remoteMap.containsKey(local.id)) {
                if (local.isSynced) {
                    database.habitDao().deleteById(local.id)
                    localMap.remove(local.id)
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

        localMap.values.filter { !it.isSynced }.forEach {
            database.habitDao().insertHabit(it.copy(isSynced = true))
        }
        deletedMap.values.forEach {
            if (!remoteMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }

        return localMap.size
    }

    private fun parseHabitJson(json: JSONObject): HabitEntity {
        val logObj = json.optJSONObject("log") ?: json.optJSONObject("logsJson") ?: JSONObject()
        return HabitEntity(
            id = json.getString("id"),
            name = json.optString("name"),
            streak = json.optInt("streak", 0),
            target = json.optString("target", "Mỗi ngày"),
            logsJson = logObj.toString(),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
            isSynced = true
        )
    }

    // ─── 7. Goals ─────────────────────────────────────────────────────────────
    private suspend fun syncGoals(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("goals", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())
        val remoteMap = remoteItems.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val localItems = database.goalDao().getAllGoals().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()
        val deletedMap = database.deletedItemDao().getByCollection("goals").associateBy { it.id }

        remoteMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val goal = parseGoalJson(json).copy(isSynced = true)
                    database.goalDao().insertGoal(goal)
                    localMap[id] = goal
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val goal = parseGoalJson(json).copy(isSynced = true)
                    database.goalDao().insertGoal(goal)
                    localMap[id] = goal
                }
            }
        }

        localItems.forEach { local ->
            if (deletedMap.containsKey(local.id)) {
                database.goalDao().deleteById(local.id)
                localMap.remove(local.id)
            } else if (!remoteMap.containsKey(local.id)) {
                if (local.isSynced) {
                    database.goalDao().deleteById(local.id)
                    localMap.remove(local.id)
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

        localMap.values.filter { !it.isSynced }.forEach {
            database.goalDao().insertGoal(it.copy(isSynced = true))
        }
        deletedMap.values.forEach {
            if (!remoteMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }

        return localMap.size
    }

    private fun parseGoalJson(json: JSONObject) = GoalEntity(
        id = json.getString("id"),
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

    // ─── 8. Journal ───────────────────────────────────────────────────────────
    private suspend fun syncJournal(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("journal", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())
        val remoteMap = remoteItems.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val localItems = database.journalDao().getAllEntries().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()
        val deletedMap = database.deletedItemDao().getByCollection("journal").associateBy { it.id }

        remoteMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val entry = parseJournalJson(json).copy(isSynced = true)
                    database.journalDao().insertEntry(entry)
                    localMap[id] = entry
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val entry = parseJournalJson(json).copy(isSynced = true)
                    database.journalDao().insertEntry(entry)
                    localMap[id] = entry
                }
            }
        }

        localItems.forEach { local ->
            if (deletedMap.containsKey(local.id)) {
                database.journalDao().deleteById(local.id)
                localMap.remove(local.id)
            } else if (!remoteMap.containsKey(local.id)) {
                if (local.isSynced) {
                    database.journalDao().deleteById(local.id)
                    localMap.remove(local.id)
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

        localMap.values.filter { !it.isSynced }.forEach {
            database.journalDao().insertEntry(it.copy(isSynced = true))
        }
        deletedMap.values.forEach {
            if (!remoteMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }

        return localMap.size
    }

    private fun parseJournalJson(json: JSONObject) = JournalEntity(
        id = json.getString("id"),
        date = json.optString("date"),
        mood = json.optString("mood", "😊"),
        body = json.optString("body"),
        title = json.optString("title"),
        updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
        isSynced = true
    )

    // ─── 9. Vocab ─────────────────────────────────────────────────────────────
    private suspend fun syncVocab(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("vocab", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())
        val remoteMap = remoteItems.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val localItems = database.vocabDao().getAllVocab().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()
        val deletedMap = database.deletedItemDao().getByCollection("vocab").associateBy { it.id }

        remoteMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val vocab = parseVocabJson(json).copy(isSynced = true)
                    database.vocabDao().insertVocab(vocab)
                    localMap[id] = vocab
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val vocab = parseVocabJson(json).copy(isSynced = true)
                    database.vocabDao().insertVocab(vocab)
                    localMap[id] = vocab
                }
            }
        }

        localItems.forEach { local ->
            if (deletedMap.containsKey(local.id)) {
                database.vocabDao().deleteById(local.id)
                localMap.remove(local.id)
            } else if (!remoteMap.containsKey(local.id)) {
                if (local.isSynced) {
                    database.vocabDao().deleteById(local.id)
                    localMap.remove(local.id)
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

        localMap.values.filter { !it.isSynced }.forEach {
            database.vocabDao().insertVocab(it.copy(isSynced = true))
        }
        deletedMap.values.forEach {
            if (!remoteMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }

        return localMap.size
    }

    private fun parseVocabJson(json: JSONObject) = VocabEntity(
        id = json.getString("id"),
        word = json.optString("word"),
        pron = json.optString("pron"),
        type = json.optString("type", "n"),
        mean = json.optString("mean"),
        example = json.optString("ex", json.optString("example")),
        srsLevel = json.optInt("srsLevel", 0),
        updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
        isSynced = true
    )

    // ─── 10. Proj Tasks ───────────────────────────────────────────────────────
    private suspend fun syncProjTasks(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("proj_tasks", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())
        val remoteMap = remoteItems.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val localItems = database.projectTaskDao().getAllTasks().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()
        val deletedMap = database.deletedItemDao().getByCollection("proj_tasks").associateBy { it.id }

        remoteMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val task = parseTaskJson(json).copy(isSynced = true)
                    database.projectTaskDao().insertTask(task)
                    localMap[id] = task
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val task = parseTaskJson(json).copy(isSynced = true)
                    database.projectTaskDao().insertTask(task)
                    localMap[id] = task
                }
            }
        }

        localItems.forEach { local ->
            if (deletedMap.containsKey(local.id)) {
                database.projectTaskDao().deleteTask(local.id)
                localMap.remove(local.id)
            } else if (!remoteMap.containsKey(local.id)) {
                if (local.isSynced) {
                    database.projectTaskDao().deleteTask(local.id)
                    localMap.remove(local.id)
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

        localMap.values.filter { !it.isSynced }.forEach {
            database.projectTaskDao().insertTask(it.copy(isSynced = true))
        }
        deletedMap.values.forEach {
            if (!remoteMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }

        return localMap.size
    }

    private fun parseTaskJson(json: JSONObject) = ProjectTaskEntity(
        id = json.getString("id"),
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

    // ─── 11. Mock Tests ───────────────────────────────────────────────────────
    private suspend fun syncMockTests(uid: String, token: String, refresh: String, onRefreshed: suspend (String, String) -> Unit): Int {
        val remoteRes = syncService.getCollectionItems("mocktests", uid, token, refresh, onRefreshed)
        val remoteItems = remoteRes.getOrDefault(emptyList())
        val remoteMap = remoteItems.filter { it.optString("id").isNotBlank() }.associateBy { it.getString("id") }

        val localItems = database.mockTestDao().getAllMockTests().first().toMutableList()
        val localMap = localItems.associateBy { it.id }.toMutableMap()
        val deletedMap = database.deletedItemDao().getByCollection("mocktests").associateBy { it.id }

        remoteMap.forEach { (id, json) ->
            val remoteUpdated = json.optLong("updatedAt", 0L)
            val tombstone = deletedMap[id]

            if (tombstone != null) {
                if (remoteUpdated > tombstone.deletedAt) {
                    database.deletedItemDao().deleteById(id)
                    val test = parseMockTestJson(json).copy(isSynced = true)
                    database.mockTestDao().insertMockTest(test)
                    localMap[id] = test
                } else {
                    localMap.remove(id)
                }
            } else {
                val local = localMap[id]
                if (local == null || remoteUpdated > local.updatedAt) {
                    val test = parseMockTestJson(json).copy(isSynced = true)
                    database.mockTestDao().insertMockTest(test)
                    localMap[id] = test
                }
            }
        }

        localItems.forEach { local ->
            if (deletedMap.containsKey(local.id)) {
                database.mockTestDao().deleteMockTest(local.id)
                localMap.remove(local.id)
            } else if (!remoteMap.containsKey(local.id)) {
                if (local.isSynced) {
                    database.mockTestDao().deleteMockTest(local.id)
                    localMap.remove(local.id)
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

        localMap.values.filter { !it.isSynced }.forEach {
            database.mockTestDao().insertMockTest(it.copy(isSynced = true))
        }
        deletedMap.values.forEach {
            if (!remoteMap.containsKey(it.id)) database.deletedItemDao().deleteById(it.id)
        }

        return localMap.size
    }

    private fun parseMockTestJson(json: JSONObject) = MockTestEntity(
        id = json.getString("id"),
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
}
