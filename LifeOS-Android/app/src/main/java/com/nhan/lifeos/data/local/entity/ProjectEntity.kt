package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val status: String = "Cần làm", // "Cần làm", "Đang làm", "Hoàn thành"
    val priority: String = "mid",   // "high", "mid", "low"
    val budget: Long = 0L,
    val due: String = "",           // "YYYY-MM-DD"
    val desc: String = "",
    val tags: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
