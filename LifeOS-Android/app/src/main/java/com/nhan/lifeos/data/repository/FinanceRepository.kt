package com.nhan.lifeos.data.repository

import com.nhan.lifeos.data.local.LifeOSDatabase
import com.nhan.lifeos.data.local.entity.GoalEntity
import com.nhan.lifeos.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class FinanceRepository(
    private val database: LifeOSDatabase,
    private val cloudSyncRepo: CloudSyncRepository? = null
) {
    private val transactionDao = database.transactionDao()
    private val goalDao = database.goalDao()

    // ─── Transactions ──────────────────────────────────────────────────────────
    fun getAllTransactions(): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun addTransaction(
        type: String, // "expense" or "income"
        categoryOrSource: String,
        amount: Long,
        date: String,
        paymentMethod: String = "Tiền mặt",
        note: String = ""
    ) = withContext(Dispatchers.IO) {
        val tx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = type,
            categoryOrSource = categoryOrSource,
            amount = amount,
            date = date,
            paymentMethod = paymentMethod,
            note = note,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(tx)
        cloudSyncRepo?.triggerAutoSync("finance")
    }

    suspend fun deleteTransaction(id: String) = withContext(Dispatchers.IO) {
        val tx = transactionDao.getTransactionById(id)
        val col = if (tx != null) {
            if (tx.type == "income") "income" else "expense"
        } else null

        if (col != null) {
            cloudSyncRepo?.recordDeletedItem(id, col)
        } else {
            cloudSyncRepo?.recordDeletedItem(id, "income")
            cloudSyncRepo?.recordDeletedItem(id, "expense")
        }
        transactionDao.deleteById(id)
        cloudSyncRepo?.triggerAutoSync("finance")
    }

    // ─── Goals ─────────────────────────────────────────────────────────────────
    fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

    suspend fun addGoal(
        title: String,
        category: String = "Tài chính",
        targetValue: Long,
        currentValue: Long = 0L,
        unit: String = "₫",
        deadline: String = "",
        note: String = ""
    ) = withContext(Dispatchers.IO) {
        val goal = GoalEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            category = category,
            targetValue = targetValue,
            currentValue = currentValue,
            unit = unit,
            deadline = deadline,
            note = note,
            updatedAt = System.currentTimeMillis()
        )
        goalDao.insertGoal(goal)
        cloudSyncRepo?.triggerAutoSync("goals")
    }

    suspend fun updateGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.updateGoal(goal.copy(updatedAt = System.currentTimeMillis()))
        cloudSyncRepo?.triggerAutoSync("goals")
    }

    suspend fun addToGoalProgress(id: String, amount: Long) = withContext(Dispatchers.IO) {
        goalDao.addToProgress(id, amount, System.currentTimeMillis())
        cloudSyncRepo?.triggerAutoSync("goals")
    }

    suspend fun deleteGoal(id: String) = withContext(Dispatchers.IO) {
        cloudSyncRepo?.recordDeletedItem(id, "goals")
        goalDao.deleteById(id)
        cloudSyncRepo?.triggerAutoSync("goals")
    }

    // ─── CSV Export ───────────────────────────────────────────────────────────
    fun generateTransactionsCsv(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("ID,Loại,Danh mục/Nguồn,Số tiền (VNĐ),Ngày,Hình thức,Ghi chú\n")
        transactions.forEach { tx ->
            val typeStr = if (tx.type == "income") "Thu nhập" else "Chi tiêu"
            val safeNote = tx.note.replace(",", " - ").replace("\n", " ")
            sb.append("\"${tx.id}\",\"$typeStr\",\"${tx.categoryOrSource}\",${tx.amount},\"${tx.date}\",\"${tx.paymentMethod}\",\"$safeNote\"\n")
        }
        return sb.toString()
    }

    // ─── Demo Seeding ─────────────────────────────────────────────────────────
    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        // Sample data disabled per user request: start completely clean
    }
}
