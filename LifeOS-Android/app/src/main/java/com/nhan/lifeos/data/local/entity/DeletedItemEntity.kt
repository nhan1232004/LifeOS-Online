package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_items")
data class DeletedItemEntity(
    @PrimaryKey
    val id: String,
    val collectionName: String,
    val deletedAt: Long = System.currentTimeMillis()
)
