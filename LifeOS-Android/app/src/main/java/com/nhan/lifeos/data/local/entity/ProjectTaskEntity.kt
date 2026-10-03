package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project_tasks")
data class ProjectTaskEntity(
    @PrimaryKey val id: String,
    val projId: String,
    val text: String,
    val status: String = "Cần làm",
    val priority: String = "mid",
    val desc: String = "",
    val start: String = "",
    val due: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
