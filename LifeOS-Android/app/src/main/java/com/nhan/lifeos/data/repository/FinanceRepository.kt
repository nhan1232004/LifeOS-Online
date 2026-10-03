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

class FinanceRepository(private val database: LifeOSDatabase) {
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
    }

    suspend fun deleteTransaction(id: String) = withContext(Dispatchers.IO) {
        transactionDao.deleteById(id)
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
    }

    suspend fun updateGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
        goalDao.updateGoal(goal.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun addToGoalProgress(id: String, amount: Long) = withContext(Dispatchers.IO) {
        goalDao.addToProgress(id, amount, System.currentTimeMillis())
    }

    suspend fun deleteGoal(id: String) = withContext(Dispatchers.IO) {
        goalDao.deleteById(id)
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
        val currentTxs = transactionDao.getAllTransactions().first()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        if (currentTxs.isEmpty()) {
            val sampleTransactions = listOf(
                TransactionEntity(
                    id = "tx-1",
                    type = "income",
                    categoryOrSource = "Lương chính",
                    amount = 25000000L,
                    date = todayStr,
                    paymentMethod = "Chuyển khoản",
                    note = "Lương tháng này nhận qua Vietcombank"
                ),
                TransactionEntity(
                    id = "tx-2",
                    type = "income",
                    categoryOrSource = "Freelance Mobile",
                    amount = 8500000L,
                    date = todayStr,
                    paymentMethod = "Chuyển khoản",
                    note = "Dự án Android Native Kotlin"
                ),
                TransactionEntity(
                    id = "tx-3",
                    type = "expense",
                    categoryOrSource = "Ăn uống",
                    amount = 125000L,
                    date = todayStr,
                    paymentMethod = "Ví điện tử",
                    note = "Ăn trưa cơm văn phòng & cafe"
                ),
                TransactionEntity(
                    id = "tx-4",
                    type = "expense",
                    categoryOrSource = "Nhà ở & Tiện ích",
                    amount = 4500000L,
                    date = todayStr,
                    paymentMethod = "Chuyển khoản",
                    note = "Tiền thuê căn hộ & điện nước"
                ),
                TransactionEntity(
                    id = "tx-5",
                    type = "expense",
                    categoryOrSource = "Mua sắm",
                    amount = 650000L,
                    date = todayStr,
                    paymentMethod = "Thẻ ngân hàng",
                    note = "Sách Clean Architecture & phụ kiện"
                ),
                TransactionEntity(
                    id = "tx-6",
                    type = "expense",
                    categoryOrSource = "Di chuyển",
                    amount = 85000L,
                    date = todayStr,
                    paymentMethod = "Tiền mặt",
                    note = "Đổ xăng xe máy"
                )
            )
            transactionDao.insertAll(sampleTransactions)
        }

        val currentGoals = goalDao.getAllGoals().first()
        if (currentGoals.isEmpty()) {
            val sampleGoals = listOf(
                GoalEntity(
                    id = "goal-1",
                    title = "Quỹ khẩn cấp 6 tháng",
                    category = "Tài chính",
                    targetValue = 60000000L,
                    currentValue = 38500000L,
                    unit = "₫",
                    deadline = "2026-12-31",
                    note = "Gửi tiết kiệm linh hoạt ngân hàng"
                ),
                GoalEntity(
                    id = "goal-2",
                    title = "Chứng chỉ Google Android Dev",
                    category = "Học tập",
                    targetValue = 100L,
                    currentValue = 75L,
                    unit = "%",
                    deadline = "2026-11-15",
                    note = "Ôn tập Jetpack Compose & Clean Architecture"
                ),
                GoalEntity(
                    id = "goal-3",
                    title = "Mua Macbook Pro M3 Max",
                    category = "Tài chính",
                    targetValue = 52000000L,
                    currentValue = 52000000L,
                    unit = "₫",
                    deadline = "2026-10-01",
                    note = "Đã tích lũy đủ 100% mục tiêu! 🎉"
                ),
                GoalEntity(
                    id = "goal-4",
                    title = "Đọc 24 cuốn sách phát triển bản thân",
                    category = "Phát triển",
                    targetValue = 24L,
                    currentValue = 18L,
                    unit = "cuốn",
                    deadline = "2026-12-31",
                    note = "Mục tiêu 2 cuốn/tháng"
                )
            )
            goalDao.insertAll(sampleGoals)
        }
    }
}
