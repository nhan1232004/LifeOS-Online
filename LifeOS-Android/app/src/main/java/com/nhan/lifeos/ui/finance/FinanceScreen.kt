package com.nhan.lifeos.ui.finance

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhan.lifeos.core.designsystem.LifeOSAmber
import com.nhan.lifeos.core.designsystem.LifeOSCyan
import com.nhan.lifeos.core.designsystem.LifeOSGlassBorder
import com.nhan.lifeos.core.designsystem.LifeOSGreen
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSRed
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceCard
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import com.nhan.lifeos.data.local.entity.TransactionEntity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinanceScreen(viewModel: FinanceViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    val filterTabs = listOf(
        TransactionFilter.ALL to "Tất cả",
        TransactionFilter.EXPENSE to "Chi tiêu",
        TransactionFilter.INCOME to "Thu nhập"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Thu chi & Tài chính",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = LifeOSTextHigh
                        )
                        Text(
                            text = "Cân đối thu chi & dòng tiền cá nhân",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LifeOSTextMid
                        )
                    }

                    // CSV Export Button
                    IconButton(
                        onClick = {
                            val csvData = viewModel.getCsvData()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, csvData)
                                putExtra(Intent.EXTRA_TITLE, "Báo cáo Thu Chi LifeOS")
                                type = "text/csv"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Xuất báo cáo CSV"))
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(LifeOSSurfaceCard)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = "Xuất CSV",
                            tint = LifeOSCyan
                        )
                    }
                }
            }

            // Net Balance Big Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        LifeOSPrimary.copy(alpha = 0.25f),
                                        LifeOSCyan.copy(alpha = 0.15f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Số dư ròng",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LifeOSTextMid
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(LifeOSGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Tiết kiệm: ${uiState.savingsRate}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LifeOSGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = formatVnd(uiState.netBalance),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = if (uiState.netBalance >= 0) LifeOSTextHigh else LifeOSRed,
                                fontSize = 32.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Income Metric Subcard
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141424))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(LifeOSGreen.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowUpward,
                                                contentDescription = null,
                                                tint = LifeOSGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Tổng thu",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LifeOSTextLow
                                            )
                                            Text(
                                                text = formatVndShort(uiState.totalIncome),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = LifeOSGreen
                                            )
                                        }
                                    }
                                }

                                // Expense Metric Subcard
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141424))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(LifeOSRed.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDownward,
                                                contentDescription = null,
                                                tint = LifeOSRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Tổng chi",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LifeOSTextLow
                                            )
                                            Text(
                                                text = formatVndShort(uiState.totalExpense),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = LifeOSRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Category Expense Breakdown (Bar Visualization)
            if (uiState.categoryExpenses.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Phân bổ chi tiêu",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LifeOSTextHigh
                                )
                                Text(
                                    text = "${uiState.categoryExpenses.size} nhóm",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LifeOSTextMid
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val totalExp = uiState.totalExpense.coerceAtLeast(1L)
                            uiState.categoryExpenses.entries.take(5).forEach { (category, amount) ->
                                val pct = ((amount.toDouble() / totalExp.toDouble()) * 100.0).toInt()
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LifeOSTextHigh,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${formatVnd(amount)} ($pct%)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = LifeOSTextMid
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (amount.toFloat() / totalExp.toFloat()).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = getCategoryColor(category),
                                        trackColor = Color(0xFF222238)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Filter Tabs
            item {
                TabRow(
                    selectedTabIndex = filterTabs.indexOfFirst { it.first == uiState.currentFilter }.coerceAtLeast(0),
                    containerColor = Color.Transparent,
                    contentColor = LifeOSPrimary,
                    divider = {},
                    indicator = { tabPositions ->
                        val index = filterTabs.indexOfFirst { it.first == uiState.currentFilter }.coerceAtLeast(0)
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                            color = LifeOSPrimary
                        )
                    }
                ) {
                    filterTabs.forEach { (filter, label) ->
                        Tab(
                            selected = uiState.currentFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (uiState.currentFilter == filter) FontWeight.Bold else FontWeight.Normal,
                                    color = if (uiState.currentFilter == filter) LifeOSPrimary else LifeOSTextMid
                                )
                            }
                        )
                    }
                }
            }

            // Transactions List
            if (uiState.transactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có giao dịch nào trong danh mục này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LifeOSTextLow
                        )
                    }
                }
            } else {
                items(uiState.transactions, key = { it.id }) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        onDelete = { viewModel.deleteTransaction(tx.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB Add Transaction
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = LifeOSPrimary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm giao dịch")
        }

        // Add Transaction Dialog
        if (showAddDialog) {
            AddTransactionDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { type, cat, amount, date, method, note ->
                    viewModel.addTransaction(type, cat, amount, date, method, note)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == "income"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isIncome) LifeOSGreen.copy(alpha = 0.15f)
                        else LifeOSRed.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Rounded.TrendingUp else getCategoryIcon(transaction.categoryOrSource),
                    contentDescription = null,
                    tint = if (isIncome) LifeOSGreen else LifeOSRed,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.categoryOrSource.ifBlank { if (isIncome) "Thu nhập khác" else "Chi tiêu khác" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LifeOSTextHigh
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = transaction.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSTextMid
                    )
                    if (transaction.paymentMethod.isNotBlank()) {
                        Text(
                            text = "• ${transaction.paymentMethod}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LifeOSTextLow
                        )
                    }
                }

                if (transaction.note.isNotBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = LifeOSTextLow,
                        maxLines = 1
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (isIncome) "+ " else "- ") + formatVnd(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isIncome) LifeOSGreen else LifeOSRed
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Xóa",
                        tint = LifeOSTextLow,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAdd: (type: String, category: String, amount: Long, date: String, method: String, note: String) -> Unit
) {
    var selectedTypeIndex by remember { mutableIntStateOf(0) } // 0: Chi tiêu, 1: Thu nhập
    var amountText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("Ăn uống") }
    var methodText by remember { mutableStateOf("Chuyển khoản") }
    var noteText by remember { mutableStateOf("") }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var dateText by remember { mutableStateOf(sdf.format(Date())) }

    val expenseCategories = listOf("Ăn uống", "Nhà ở & Tiện ích", "Mua sắm", "Di chuyển", "Giải trí", "Y tế", "Giáo dục", "Khác")
    val incomeSources = listOf("Lương chính", "Thưởng", "Freelance", "Đầu tư", "Kinh doanh", "Khác")
    val paymentMethods = listOf("Chuyển khoản", "Tiền mặt", "Ví điện tử", "Thẻ tín dụng")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Thêm giao dịch mới",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextHigh
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type Switcher Tabs
                TabRow(
                    selectedTabIndex = selectedTypeIndex,
                    containerColor = Color.Transparent,
                    contentColor = if (selectedTypeIndex == 0) LifeOSRed else LifeOSGreen
                ) {
                    Tab(
                        selected = selectedTypeIndex == 0,
                        onClick = {
                            selectedTypeIndex = 0
                            categoryText = expenseCategories.first()
                        },
                        text = { Text("Chi tiêu (-)", color = if (selectedTypeIndex == 0) LifeOSRed else LifeOSTextMid, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTypeIndex == 1,
                        onClick = {
                            selectedTypeIndex = 1
                            categoryText = incomeSources.first()
                        },
                        text = { Text("Thu nhập (+)", color = if (selectedTypeIndex == 1) LifeOSGreen else LifeOSTextMid, fontWeight = FontWeight.Bold) }
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Số tiền (VNĐ)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (selectedTypeIndex == 0) LifeOSRed else LifeOSGreen,
                        unfocusedBorderColor = LifeOSGlassBorder,
                        focusedLabelColor = if (selectedTypeIndex == 0) LifeOSRed else LifeOSGreen
                    )
                )

                // Category Chips
                Text(
                    text = if (selectedTypeIndex == 0) "Danh mục chi tiêu:" else "Nguồn thu nhập:",
                    style = MaterialTheme.typography.labelSmall,
                    color = LifeOSTextMid
                )
                val currentCats = if (selectedTypeIndex == 0) expenseCategories else incomeSources
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(currentCats) { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (categoryText == cat) LifeOSPrimary.copy(alpha = 0.3f)
                                    else LifeOSSurfaceCard
                                )
                                .clickable { categoryText = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (categoryText == cat) LifeOSPrimary else LifeOSTextMid,
                                fontWeight = if (categoryText == cat) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Payment Method Chips
                Text(
                    text = "Hình thức thanh toán:",
                    style = MaterialTheme.typography.labelSmall,
                    color = LifeOSTextMid
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(paymentMethods) { method ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (methodText == method) LifeOSCyan.copy(alpha = 0.3f)
                                    else LifeOSSurfaceCard
                                )
                                .clickable { methodText = method }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = method,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (methodText == method) LifeOSCyan else LifeOSTextMid,
                                fontWeight = if (methodText == method) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Date & Note
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Ngày (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Ghi chú (tùy chọn)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    if (amt > 0) {
                        val type = if (selectedTypeIndex == 0) "expense" else "income"
                        onAdd(type, categoryText, amt, dateText, methodText, noteText)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTypeIndex == 0) LifeOSRed else LifeOSGreen
                )
            ) {
                Text("Lưu giao dịch", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LifeOSTextMid)
            }
        },
        containerColor = Color(0xFF141424),
        shape = RoundedCornerShape(20.dp)
    )
}

// Helpers
fun formatVnd(amount: Long): String {
    val formatter = DecimalFormat("#,###")
    return "${formatter.format(amount)} ₫"
}

fun formatVndShort(amount: Long): String {
    return when {
        amount >= 1_000_000_000L -> "${(amount / 100_000_000).toDouble() / 10} tỷ"
        amount >= 1_000_000L -> "${(amount / 100_000).toDouble() / 10} tr"
        amount >= 1_000L -> "${amount / 1_000}k"
        else -> "$amount ₫"
    }
}

fun getCategoryIcon(cat: String): ImageVector {
    return when {
        cat.contains("Ăn") || cat.contains("Cafe") -> Icons.Rounded.LocalCafe
        cat.contains("Mua") || cat.contains("Sắm") -> Icons.Rounded.ShoppingBag
        cat.contains("Nhà") -> Icons.Rounded.Wallet
        else -> Icons.Rounded.Payment
    }
}

fun getCategoryColor(cat: String): Color {
    return when {
        cat.contains("Ăn") -> LifeOSAmber
        cat.contains("Nhà") -> LifeOSCyan
        cat.contains("Mua") -> Color(0xFFCE93D8)
        cat.contains("Di chuyển") -> Color(0xFFFF80AB)
        cat.contains("Y tế") -> LifeOSRed
        else -> LifeOSPrimary
    }
}
