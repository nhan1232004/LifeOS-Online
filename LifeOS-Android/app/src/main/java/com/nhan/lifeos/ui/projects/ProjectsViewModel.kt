package com.nhan.lifeos.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.ProjectEntity
import com.nhan.lifeos.data.local.entity.ProjectTaskEntity
import com.nhan.lifeos.data.repository.TaskTimeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val projects: List<ProjectEntity> = emptyList(),
    val selectedProjectId: String? = null,
    val selectedProject: ProjectEntity? = null,
    val allTasks: List<ProjectTaskEntity> = emptyList(),
    val displayTasks: List<ProjectTaskEntity> = emptyList(),
    val taskFilter: String = "all", // "all", "Cần làm", "Đang làm", "Hoàn thành"
    val projectProgress: Int = 0,
    val completedTasksCount: Int = 0,
    val totalTasksCount: Int = 0
)

class ProjectsViewModel(private val repository: TaskTimeRepository) : ViewModel() {

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    private val _taskFilter = MutableStateFlow("all")

    val uiState: StateFlow<ProjectsUiState> = combine(
        repository.allProjects,
        repository.allProjectTasks,
        _selectedProjectId,
        _taskFilter
    ) { projects, allTasks, selectedId, filter ->
        val activeProj = projects.find { it.id == selectedId } ?: projects.firstOrNull()
        val currentProjId = activeProj?.id

        val relevantTasks = if (currentProjId != null) {
            allTasks.filter { it.projId == currentProjId }
        } else {
            allTasks
        }

        val completedCount = relevantTasks.count { it.status == "Hoàn thành" }
        val totalCount = relevantTasks.size
        val progressPercent = if (totalCount > 0) {
            (completedCount * 100) / totalCount
        } else {
            activeProj?.progress ?: 0
        }

        val display = when (filter) {
            "all" -> relevantTasks
            "Cần làm" -> relevantTasks.filter { it.status == "Cần làm" || it.status == "Khởi tạo" || it.status == "Backlog" }
            "Đang làm" -> relevantTasks.filter { it.status == "Đang làm" || it.status == "In Progress" }
            "Hoàn thành" -> relevantTasks.filter { it.status == "Hoàn thành" || it.status == "Done" }
            else -> relevantTasks.filter { it.status == filter }
        }

        ProjectsUiState(
            projects = projects,
            selectedProjectId = currentProjId,
            selectedProject = activeProj,
            allTasks = relevantTasks,
            displayTasks = display,
            taskFilter = filter,
            projectProgress = progressPercent,
            completedTasksCount = completedCount,
            totalTasksCount = totalCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProjectsUiState()
    )

    fun selectProject(id: String?) {
        _selectedProjectId.value = id
    }

    fun setTaskFilter(filter: String) {
        _taskFilter.value = filter
    }

    fun addProject(
        name: String,
        status: String = "Cần làm",
        priority: String = "high",
        budget: Long = 0L,
        due: String = "",
        desc: String = ""
    ) {
        viewModelScope.launch {
            repository.insertProject(
                name = name,
                status = status,
                priority = priority,
                budget = budget,
                due = due,
                desc = desc
            )
        }
    }

    fun updateProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.updateProject(project)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            if (_selectedProjectId.value == id) {
                _selectedProjectId.value = null
            }
        }
    }

    fun addProjectTask(
        projId: String,
        text: String,
        status: String = "Cần làm",
        priority: String = "mid",
        desc: String = "",
        due: String = ""
    ) {
        viewModelScope.launch {
            repository.insertProjectTask(
                projId = projId,
                text = text,
                status = status,
                priority = priority,
                desc = desc,
                start = "",
                due = due
            )
        }
    }

    fun cycleTaskStatus(task: ProjectTaskEntity) {
        val nextStatus = when (task.status) {
            "Khởi tạo", "Backlog", "Cần làm" -> "Đang làm"
            "Đang làm" -> "Hoàn thành"
            "Hoàn thành" -> "Cần làm"
            else -> "Cần làm"
        }
        viewModelScope.launch {
            repository.updateProjectTaskStatus(task.id, nextStatus)
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            repository.deleteProjectTask(id)
        }
    }

    fun getProjectMessages(projId: String): kotlinx.coroutines.flow.Flow<List<com.nhan.lifeos.data.local.entity.ProjectMessageEntity>> {
        return repository.getProjectMessages(projId)
    }

    fun addMember(project: ProjectEntity, email: String) {
        viewModelScope.launch {
            repository.addProjectMember(project, email)
        }
    }

    fun removeMember(project: ProjectEntity, email: String) {
        viewModelScope.launch {
            repository.removeProjectMember(project, email)
        }
    }

    fun sendChatMessage(projId: String, senderEmail: String, senderName: String, text: String) {
        viewModelScope.launch {
            repository.sendProjectMessage(projId, senderEmail, senderName, text)
        }
    }
}
