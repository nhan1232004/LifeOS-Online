package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal")
data class JournalEntity(
    @PrimaryKey
    val id: String,
    val date: String,             // "YYYY-MM-DD"
    val mood: String = "😊",       // "😊", "🔥", "💡", "💪", "😴", "🌧️"
    val body: String,
    val title: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
