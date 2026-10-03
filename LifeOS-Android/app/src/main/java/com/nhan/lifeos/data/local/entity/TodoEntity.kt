package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val priority: String, // "high", "mid", "low"
    val date: String,     // "YYYY-MM-DD"
    val note: String = "",
    val done: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
