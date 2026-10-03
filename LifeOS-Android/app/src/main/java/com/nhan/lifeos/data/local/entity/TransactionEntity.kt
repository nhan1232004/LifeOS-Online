package com.nhan.lifeos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    val type: String,               // "expense" or "income"
    val categoryOrSource: String,   // e.g. "Ăn uống", "Lương", "Freelance"
    val amount: Long,               // in VND
    val date: String,               // "YYYY-MM-DD"
    val paymentMethod: String = "", // "Tiền mặt", "Thẻ ngân hàng", "Ví điện tử"
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
