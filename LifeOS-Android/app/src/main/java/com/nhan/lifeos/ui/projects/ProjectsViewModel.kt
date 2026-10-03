package com.nhan.lifeos.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhan.lifeos.data.local.entity.ProjectEntity
import com.nhan.lifeos.data.repository.TaskTimeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KanbanBoardState(
    val todoProjects: List<ProjectEntity> = emptyList(),
    val inProgressProjects: List<ProjectEntity> = emptyList(),
    val doneProjects: List<ProjectEntity> = emptyList(),
    val totalCount: Int = 0
)

class ProjectsViewModel(private val repository: TaskTimeRepository) : ViewModel() {

    val uiState: StateFlow<KanbanBoardState> = repository.allProjects.map { projects ->
        KanbanBoardState(
            todoProjects = projects.filter { it.status == "Cần làm" },
            inProgressProjects = projects.filter { it.status == "Đang làm" },
            doneProjects = projects.filter { it.status == "Hoàn thành" },
            totalCount = projects.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = KanbanBoardState()
    )

    fun moveProject(id: String, currentStatus: String) {
        val nextStatus = when (currentStatus) {
            "Cần làm" -> "Đang làm"
            "Đang làm" -> "Hoàn thành"
            "Hoàn thành" -> "Cần làm"
            else -> "Đang làm"
        }
        viewModelScope.launch {
            repository.updateProjectStatus(id, nextStatus)
        }
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

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }
}
