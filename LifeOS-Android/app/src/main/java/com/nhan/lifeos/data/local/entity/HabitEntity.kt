package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val streak: Int = 0,
    val target: String = "Mỗi ngày",
    val logsJson: String = "{}", // Map of "YYYY-MM-DD" -> true
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
