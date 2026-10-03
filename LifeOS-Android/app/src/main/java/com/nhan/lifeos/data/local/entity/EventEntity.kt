package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val dateStart: String, // "YYYY-MM-DD"
    val dateEnd: String,   // "YYYY-MM-DD"
    val timeStart: String = "", // "HH:mm"
    val timeEnd: String = "",   // "HH:mm"
    val type: String = "work",  // "work", "personal", "health", "social", "finance", "study"
    val desc: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
