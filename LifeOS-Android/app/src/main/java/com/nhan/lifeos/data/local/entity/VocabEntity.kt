package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocab")
data class VocabEntity(
    @PrimaryKey
    val id: String,
    val word: String,
    val pron: String = "",
    val type: String = "n",       // n, v, adj, adv, idiom
    val mean: String,
    val example: String = "",
    val srsLevel: Int = 0,        // 0: New, 1: Learning, 2: Review, 3: Mastered
    val nextReviewAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
