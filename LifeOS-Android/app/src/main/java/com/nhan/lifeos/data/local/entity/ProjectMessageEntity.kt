package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "project_messages")
data class ProjectMessageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val projId: String,
    val senderEmail: String,
    val senderName: String = "",
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
