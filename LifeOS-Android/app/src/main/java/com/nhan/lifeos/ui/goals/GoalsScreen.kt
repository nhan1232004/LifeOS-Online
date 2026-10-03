package com.nhan.lifeos.ui.goals

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.TrackChanges
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.nhan.lifeos.data.local.entity.GoalEntity
import java.text.DecimalFormat

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedGoalForProgress by remember { mutableStateOf<GoalEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LifeOSSurfaceCard)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = LifeOSTextHigh,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Mục tiêu (Goals)",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = LifeOSTextHigh
                        )
                        Text(
                            text = "Kế hoạch & Động lực phát triển dài hạn",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LifeOSTextMid
                        )
                    }
                }
            }

            // Overview Summary Card
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
                                        LifeOSCyan.copy(alpha = 0.2f),
                                        LifeOSPrimary.copy(alpha = 0.25f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Tiến độ mục tiêu",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LifeOSTextMid
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${uiState.completedGoals} / ${uiState.totalGoals} hoàn thành",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LifeOSTextHigh
                                )
                                Text(
                                    text = "Đang tích lũy: ${uiState.inProgressGoals} mục tiêu",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LifeOSCyan
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(LifeOSCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TrackChanges,
                                    contentDescription = null,
                                    tint = LifeOSCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Goals List
            if (uiState.goals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có mục tiêu nào. Nhấn + để tạo mục tiêu đầu tiên!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LifeOSTextLow
                        )
                    }
                }
            } else {
                items(uiState.goals, key = { it.id }) { goal ->
                    GoalCardItem(
                        goal = goal,
                        onAddProgress = { selectedGoalForProgress = goal },
                        onDelete = { viewModel.deleteGoal(goal.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB Add Goal
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = LifeOSCyan,
            contentColor = Color.Black,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm mục tiêu")
        }

        // Add Goal Dialog
        if (showAddDialog) {
            AddGoalDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, cat, target, current, unit, deadline, note ->
                    viewModel.addGoal(title, cat, target, current, unit, deadline, note)
                    showAddDialog = false
                }
            )
        }

        // Add Progress Dialog
        selectedGoalForProgress?.let { goal ->
            AddProgressDialog(
                goal = goal,
                onDismiss = { selectedGoalForProgress = null },
                onConfirm = { addedAmt ->
                    viewModel.addToProgress(goal.id, addedAmt)
                    selectedGoalForProgress = null
                }
            )
        }
    }
}

@Composable
fun GoalCardItem(
    goal: GoalEntity,
    onAddProgress: () -> Unit,
    onDelete: () -> Unit
) {
    val pct = goal.progressPercentage
    val isDone = goal.isCompleted

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(getGoalCategoryColor(goal.category).copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = goal.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = getGoalCategoryColor(goal.category),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isDone) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = LifeOSGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đã đạt 100% 🎉",
                            style = MaterialTheme.typography.labelSmall,
                            color = LifeOSGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (goal.deadline.isNotBlank()) {
                    Text(
                        text = "Hạn: ${goal.deadline}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSTextLow
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextHigh
            )

            if (goal.note.isNotBlank()) {
                Text(
                    text = goal.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = LifeOSTextLow,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${formatGoalValue(goal.currentValue, goal.unit)} / ${formatGoalValue(goal.targetValue, goal.unit)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LifeOSTextHigh
                )
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDone) LifeOSGreen else LifeOSCyan
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { (pct / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isDone) LifeOSGreen else LifeOSCyan,
                trackColor = Color(0xFF222238)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddProgress,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOSCyan.copy(alpha = 0.2f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = LifeOSCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Cộng dồn",
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSCyan,
                        fontWeight = FontWeight.Bold
                    )
                }

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
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, category: String, target: Long, current: Long, unit: String, deadline: String, note: String) -> Unit
) {
    var titleText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("Tài chính") }
    var targetText by remember { mutableStateOf("") }
    var currentText by remember { mutableStateOf("0") }
    var unitText by remember { mutableStateOf("₫") }
    var deadlineText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    val categories = listOf("Tài chính", "Học tập", "Sự nghiệp", "Sức khỏe", "Phát triển")
    val units = listOf("₫", "%", "cuốn", "giờ", "bài", "km")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Thêm mục tiêu mới",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextHigh
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Tên mục tiêu (VD: Quỹ 6 tháng)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSCyan,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                // Category Chips
                Text(
                    text = "Lĩnh vực:",
                    style = MaterialTheme.typography.labelSmall,
                    color = LifeOSTextMid
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (categoryText == cat) LifeOSCyan.copy(alpha = 0.3f) else LifeOSSurfaceCard)
                                .clickable {
                                    categoryText = cat
                                    if (cat == "Tài chính") unitText = "₫"
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (categoryText == cat) LifeOSCyan else LifeOSTextMid,
                                fontWeight = if (categoryText == cat) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Target and Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Mục tiêu") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LifeOSCyan,
                            unfocusedBorderColor = LifeOSGlassBorder
                        )
                    )

                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it },
                        label = { Text("Đơn vị") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LifeOSCyan,
                            unfocusedBorderColor = LifeOSGlassBorder
                        )
                    )
                }

                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Đã có hiện tại") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSCyan,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                OutlinedTextField(
                    value = deadlineText,
                    onValueChange = { deadlineText = it },
                    label = { Text("Hạn chót (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSCyan,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Ghi chú động lực") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSCyan,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.toLongOrNull() ?: 0L
                    val current = currentText.toLongOrNull() ?: 0L
                    if (titleText.isNotBlank() && target > 0) {
                        onAdd(titleText, categoryText, target, current, unitText, deadlineText, noteText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSCyan)
            ) {
                Text("Tạo mục tiêu", color = Color.Black, fontWeight = FontWeight.Bold)
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

@Composable
fun AddProgressDialog(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onConfirm: (addedAmount: Long) -> Unit
) {
    var addedAmountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cộng dồn tiến độ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextHigh
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Mục tiêu: ${goal.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LifeOSTextMid
                )
                OutlinedTextField(
                    value = addedAmountText,
                    onValueChange = { addedAmountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Số lượng / tiền cộng thêm (${goal.unit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSCyan,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = addedAmountText.toLongOrNull() ?: 0L
                    if (amt > 0) {
                        onConfirm(amt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSCyan)
            ) {
                Text("Xác nhận", color = Color.Black, fontWeight = FontWeight.Bold)
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

fun formatGoalValue(value: Long, unit: String): String {
    val formatter = DecimalFormat("#,###")
    return "${formatter.format(value)} $unit"
}

fun getGoalCategoryColor(category: String): Color {
    return when (category) {
        "Tài chính" -> LifeOSGreen
        "Học tập" -> LifeOSCyan
        "Sự nghiệp" -> LifeOSPrimary
        "Sức khỏe" -> LifeOSRed
        else -> LifeOSAmber
    }
}
