package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mock_tests")
data class MockTestEntity(
    @PrimaryKey val id: String,
    val name: String,
    val date: String,
    val list: Double = 0.0,
    val read: Double = 0.0,
    val speak: Double = 0.0,
    val write: Double = 0.0,
    val total: Double = 0.0,
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
