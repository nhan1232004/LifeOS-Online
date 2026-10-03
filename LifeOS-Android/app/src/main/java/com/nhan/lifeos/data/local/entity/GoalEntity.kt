package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String = "Tài chính", // "Tài chính", "Học tập", "Sự nghiệp", "Sức khỏe", "Khác"
    val targetValue: Long,               // Target amount or metric
    val currentValue: Long = 0L,         // Current saved / achieved
    val unit: String = "₫",              // "₫", "cuốn", "giờ", "%"
    val deadline: String = "",           // "YYYY-MM-DD"
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    val progressPercentage: Int
        get() = if (targetValue > 0) {
            ((currentValue.toDouble() / targetValue.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else 0

    val isCompleted: Boolean
        get() = currentValue >= targetValue && targetValue > 0
}
