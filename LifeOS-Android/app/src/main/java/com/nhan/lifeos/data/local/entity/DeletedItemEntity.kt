package com.nhan.lifeos.data.local.entity

import androidx.room.Entity

@Entity(tableName = "deleted_items", primaryKeys = ["id", "collectionName"])
data class DeletedItemEntity(
    val id: String,
    val collectionName: String,
    val deletedAt: Long = System.currentTimeMillis()
)

