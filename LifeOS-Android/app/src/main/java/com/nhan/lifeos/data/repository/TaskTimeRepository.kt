package com.nhan.lifeos.data.repository

import com.nhan.lifeos.data.local.LifeOSDatabase
import com.nhan.lifeos.data.local.entity.EventEntity
import com.nhan.lifeos.data.local.entity.ProjectEntity
import com.nhan.lifeos.data.local.entity.TodoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class TaskTimeRepository(private val database: LifeOSDatabase) {
    private val todoDao = database.todoDao()
    private val eventDao = database.eventDao()
    private val projectDao = database.projectDao()

    val allTodos: Flow<List<TodoEntity>> = todoDao.getAllTodos()
    val allEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getTodosForDate(date: String): Flow<List<TodoEntity>> = todoDao.getTodosByDate(date)
    fun getEventsForDate(date: String): Flow<List<EventEntity>> = eventDao.getEventsForDate(date)

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
            done = false
        )
        todoDao.insertTodo(todo)
    }

    suspend fun toggleTodo(id: String, isDone: Boolean) {
        val currentTodos = todoDao.getAllTodos().first()
        val todo = currentTodos.find { it.id == id } ?: return
        todoDao.updateTodo(todo.copy(done = isDone, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTodo(id: String) {
        todoDao.deleteById(id)
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
            desc = desc
        )
        eventDao.insertEvent(event)
    }

    suspend fun deleteEvent(id: String) {
        eventDao.deleteById(id)
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
            desc = desc
        )
        projectDao.insertProject(project)
    }

    suspend fun updateProjectStatus(id: String, status: String) {
        projectDao.updateProjectStatus(id, status)
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteById(id)
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
