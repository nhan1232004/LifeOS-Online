package com.nhan.lifeos.data.repository

import com.nhan.lifeos.data.local.LifeOSDatabase
import com.nhan.lifeos.data.local.entity.EventEntity
import com.nhan.lifeos.data.local.entity.ProjectEntity
import com.nhan.lifeos.data.local.entity.ProjectMessageEntity
import com.nhan.lifeos.data.local.entity.ProjectTaskEntity
import com.nhan.lifeos.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class TaskTimeRepository(
    private val database: LifeOSDatabase,
    private val cloudSyncRepo: CloudSyncRepository? = null
) {
    private val todoDao = database.todoDao()
    private val eventDao = database.eventDao()
    private val projectDao = database.projectDao()
    private val projectTaskDao = database.projectTaskDao()
    private val projectMessageDao = database.projectMessageDao()

    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val allProjectTasks: Flow<List<ProjectTaskEntity>> = projectTaskDao.getAllTasks()

    fun getTodosForDate(date: String): Flow<List<TodoEntity>> = todoDao.getTodosByDate(date)
    fun getEventsForDate(date: String): Flow<List<EventEntity>> = eventDao.getEventsForDate(date)
    fun getTasksForProject(projId: String): Flow<List<ProjectTaskEntity>> = projectTaskDao.getTasksByProject(projId)

    suspend fun insertTodo(
        text: String,
        priority: String = "mid",
        date: String,
        note: String = ""
    ) {
        val todo = TodoEntity(
            id = UUID.randomUUID().toString(),
            text = text,
            priority = priority,
            date = date,
            note = note,
            done = false,
            updatedAt = System.currentTimeMillis()
        )
        todoDao.insertTodo(todo)
        cloudSyncRepo?.triggerAutoSync("todos")
    }

    suspend fun toggleTodo(id: String, isDone: Boolean) {
        val currentTodos = todoDao.getAllTodos().first()
        val todo = currentTodos.find { it.id == id } ?: return
        todoDao.updateTodo(todo.copy(done = isDone, updatedAt = System.currentTimeMillis()))
        cloudSyncRepo?.triggerAutoSync("todos")
    }

    suspend fun deleteTodo(id: String) {
        cloudSyncRepo?.recordDeletedItem(id, "todos")
        todoDao.deleteById(id)
        cloudSyncRepo?.triggerAutoSync("todos")
    }

    suspend fun insertEvent(
        title: String,
        dateStart: String,
        dateEnd: String = dateStart,
        timeStart: String = "",
        timeEnd: String = "",
        type: String = "work",
        desc: String = ""
    ) {
        val event = EventEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            dateStart = dateStart,
            dateEnd = dateEnd,
            timeStart = timeStart,
            timeEnd = timeEnd,
            type = type,
            desc = desc,
            updatedAt = System.currentTimeMillis()
        )
        eventDao.insertEvent(event)
        cloudSyncRepo?.triggerAutoSync("events")
    }

    suspend fun deleteEvent(id: String) {
        cloudSyncRepo?.recordDeletedItem(id, "events")
        eventDao.deleteById(id)
        cloudSyncRepo?.triggerAutoSync("events")
    }

    suspend fun updateEvent(event: EventEntity) {
        eventDao.updateEvent(event.copy(updatedAt = System.currentTimeMillis()))
        cloudSyncRepo?.triggerAutoSync("events")
    }

    suspend fun insertProject(
        name: String,
        status: String = "Cần làm",
        priority: String = "high",
        budget: Long = 0L,
        due: String = "",
        desc: String = ""
    ) {
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            status = status,
            priority = priority,
            budget = budget,
            due = due,
            desc = desc,
            updatedAt = System.currentTimeMillis()
        )
        projectDao.insertProject(project)
        cloudSyncRepo?.triggerAutoSync("projects")
    }

    suspend fun updateProjectStatus(id: String, status: String) {
        projectDao.updateProjectStatus(id, status)
        cloudSyncRepo?.triggerAutoSync("projects")
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
        cloudSyncRepo?.triggerAutoSync("projects")
    }

    suspend fun deleteProject(id: String) {
        cloudSyncRepo?.recordDeletedItem(id, "projects")
        projectDao.deleteById(id)
        projectTaskDao.deleteTasksByProject(id)
        projectMessageDao.deleteMessagesForProject(id)
        cloudSyncRepo?.triggerAutoSync("projects")
    }

    suspend fun addProjectMember(project: ProjectEntity, email: String) {
        val trimmed = email.trim().lowercase()
        if (trimmed.isNotBlank() && !project.members.contains(trimmed)) {
            val updated = project.copy(
                members = project.members + trimmed,
                updatedAt = System.currentTimeMillis(),
                isSynced = false
            )
            projectDao.insertProject(updated)
            cloudSyncRepo?.triggerAutoSync("projects")
        }
    }

    suspend fun removeProjectMember(project: ProjectEntity, email: String) {
        val updated = project.copy(
            members = project.members.filter { it != email },
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        projectDao.insertProject(updated)
        cloudSyncRepo?.triggerAutoSync("projects")
    }

    fun getProjectMessages(projId: String): Flow<List<ProjectMessageEntity>> =
        projectMessageDao.getMessagesForProject(projId)

    suspend fun sendProjectMessage(projId: String, senderEmail: String, senderName: String, text: String) {
        if (text.isBlank()) return
        val msg = ProjectMessageEntity(
            projId = projId,
            senderEmail = senderEmail,
            senderName = senderName.ifBlank { senderEmail.substringBefore("@") },
            text = text.trim(),
            timestamp = System.currentTimeMillis()
        )
        projectMessageDao.insertMessage(msg)
    }

    suspend fun insertProjectTask(
        projId: String,
        text: String,
        status: String = "Cần làm",
        priority: String = "mid",
        desc: String = "",
        start: String = "",
        due: String = ""
    ) {
        val task = ProjectTaskEntity(
            id = UUID.randomUUID().toString(),
            projId = projId,
            text = text,
            status = status,
            priority = priority,
            desc = desc,
            start = start,
            due = due,
            updatedAt = System.currentTimeMillis()
        )
        projectTaskDao.insertTask(task)
        cloudSyncRepo?.triggerAutoSync("proj_tasks")
    }

    suspend fun updateProjectTaskStatus(id: String, status: String) {
        projectTaskDao.updateTaskStatus(id, status)
        cloudSyncRepo?.triggerAutoSync("proj_tasks")
    }

    suspend fun deleteProjectTask(id: String) {
        cloudSyncRepo?.recordDeletedItem(id, "proj_tasks")
        projectTaskDao.deleteTask(id)
        cloudSyncRepo?.triggerAutoSync("proj_tasks")
    }

    suspend fun seedSampleDataIfEmpty() {
        val existingTodos = todoDao.getAllTodos().first()
        if (existingTodos.isNotEmpty()) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowStr = sdf.format(cal.time)

        // Seed Sample Todos
        val sampleTodos = listOf(
            TodoEntity(id = "t1", text = "Tối ưu hóa Performance & Lighthouse LifeOS (≥ 90 score)", priority = "high", date = todayStr, note = "Kiểm tra bundle size và lazy loading", done = true),
            TodoEntity(id = "t2", text = "Hoàn thiện bản thiết kế UI Focused Glass Jetpack Compose", priority = "high", date = todayStr, note = "Chuẩn hóa design tokens và icons", done = true),
            TodoEntity(id = "t3", text = "Đánh giá và review Pull Request cho team", priority = "high", date = todayStr, note = "Module Kanban drag-and-drop", done = false),
            TodoEntity(id = "t4", text = "Luyện 20 từ vựng IELTS chuyên ngành công nghệ", priority = "mid", date = todayStr, note = "Bộ flashcard LifeOS", done = false),
            TodoEntity(id = "t5", text = "Thanh toán tiền điện & Internet tháng này", priority = "low", date = tomorrowStr, note = "Chuyển khoản qua app ngân hàng", done = false)
        )
        todoDao.insertAll(sampleTodos)

        // Seed Sample Events
        val sampleEvents = listOf(
            EventEntity(id = "e1", title = "Họp chiến lược sản phẩm Q3", dateStart = todayStr, dateEnd = todayStr, timeStart = "09:00", timeEnd = "10:30", type = "work", desc = "Đánh giá tiến độ ra mắt tính năng AI và Mobile App"),
            EventEntity(id = "e2", title = "Tập Gym & Cardio", dateStart = todayStr, dateEnd = todayStr, timeStart = "17:30", timeEnd = "19:00", type = "health", desc = "Buổi tập ngực và chạy bộ 5km"),
            EventEntity(id = "e3", title = "Review Code cùng Senior Dev", dateStart = tomorrowStr, dateEnd = tomorrowStr, timeStart = "14:00", timeEnd = "15:00", type = "work", desc = "Kiến trúc Clean Architecture Android Native")
        )
        eventDao.insertAll(sampleEvents)

        // Seed Sample Projects
        val sampleProjects = listOf(
            ProjectEntity(id = "p1", name = "LifeOS Android Native App", status = "Đang làm", priority = "high", budget = 15000000L, due = tomorrowStr, desc = "Ứng dụng quản lý toàn diện Native Jetpack Compose"),
            ProjectEntity(id = "p2", name = "Hệ thống Quản lý Bán lẻ Cloud", status = "Cần làm", priority = "mid", budget = 45000000L, due = tomorrowStr, desc = "Dự án thương mại điện tử đồng bộ đa kênh"),
            ProjectEntity(id = "p3", name = "Nghiên cứu Gemini 1.5 Flash AI API", status = "Hoàn thành", priority = "low", budget = 5000000L, due = todayStr, desc = "Tích hợp trợ lý AI trực tiếp vào app")
        )
        sampleProjects.forEach { projectDao.insertProject(it) }
    }
}
