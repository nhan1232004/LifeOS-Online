package com.nhan.lifeos.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextDecoration
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
import com.nhan.lifeos.data.local.entity.EventEntity
import com.nhan.lifeos.data.local.entity.TodoEntity

@Composable
fun TodayScreen(viewModel: TodayViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showQuickAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Top Header Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hôm nay",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = LifeOSTextHigh
                        )
                        Text(
                            text = uiState.currentDateFormatted.ifBlank { "Lịch trình hôm nay" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = LifeOSTextMid
                        )
                    }

                    // User Avatar Pill
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(LifeOSPrimary, LifeOSCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "N",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                }
            }

            // Productivity KPI Card (Connected to Real Room DB)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.FlashOn,
                                    contentDescription = null,
                                    tint = LifeOSCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Năng suất hôm nay",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LifeOSTextHigh
                                )
                            }
                            Text(
                                text = "${uiState.productivityPercentage}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.productivityPercentage >= 80) LifeOSGreen else LifeOSAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem(label = "Việc hoàn thành", value = "${uiState.completedCount}/${uiState.totalTodoCount}")
                            StatItem(label = "Sự kiện hôm nay", value = "${uiState.events.size}")
                            StatItem(label = "Streak thói quen", value = "12 ngày")
                        }
                    }
                }
            }

            // Quick Agenda Section Title
            item {
                Text(
                    text = "Lịch trình hôm nay (${uiState.events.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = LifeOSTextHigh,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.events.isEmpty()) {
                item {
                    Text(
                        text = "Không có sự kiện nào trong ngày hôm nay",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextLow,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(uiState.events, key = { it.id }) { event ->
                    val typeColor = when (event.type) {
                        "work" -> LifeOSPrimary
                        "study" -> LifeOSCyan
                        "health" -> LifeOSGreen
                        "social" -> Color(0xFFCE93D8)
                        else -> LifeOSAmber
                    }
                    val timeStr = if (event.timeStart.isNotBlank()) {
                        if (event.timeEnd.isNotBlank()) "${event.timeStart} - ${event.timeEnd}" else event.timeStart
                    } else "Cả ngày"

                    AgendaItemCard(
                        time = timeStr,
                        title = event.title,
                        type = event.type.replaceFirstChar { it.uppercase() },
                        typeColor = typeColor
                    )
                }
            }

            // Today's Todos Section Title
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Việc cần làm hôm nay (${uiState.todos.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = LifeOSTextHigh,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.todos.isEmpty()) {
                item {
                    Text(
                        text = "Chưa có việc nào được giao cho ngày hôm nay",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextLow,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(uiState.todos, key = { it.id }) { todo ->
                    TodayTodoItem(
                        todo = todo,
                        onToggle = { viewModel.toggleTodo(todo.id, !todo.done) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Quick Add FAB
        FloatingActionButton(
            onClick = { showQuickAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = LifeOSPrimary,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm nhanh việc")
        }
    }

    if (showQuickAddDialog) {
        QuickAddTodoDialog(
            onDismiss = { showQuickAddDialog = false },
            onConfirm = { text, pri ->
                viewModel.addQuickTodo(text, pri)
                showQuickAddDialog = false
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LifeOSTextHigh)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = LifeOSTextLow)
    }
}

@Composable
private fun AgendaItemCard(
    time: String,
    title: String,
    type: String,
    typeColor: Color
) {
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
            Box(
                modifier = Modifier
                    .size(4.dp, 36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(typeColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = LifeOSTextHigh
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LifeOSTextMid
                )
            }
            Text(
                text = type,
                style = MaterialTheme.typography.labelSmall,
                color = typeColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TodayTodoItem(
    todo: TodoEntity,
    onToggle: () -> Unit
) {
    val priColor = when (todo.priority.lowercase()) {
        "high" -> LifeOSRed
        "mid" -> LifeOSAmber
        else -> LifeOSGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.done,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = LifeOSPrimary,
                    uncheckedColor = LifeOSTextLow,
                    checkmarkColor = Color.White
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (todo.done) LifeOSTextLow else LifeOSTextHigh,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None
                )
                if (todo.note.isNotBlank()) {
                    Text(
                        text = todo.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSTextLow
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(priColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = todo.priority.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = priColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickAddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, priority: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("high") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Thêm việc hôm nay", fontWeight = FontWeight.Bold, color = LifeOSTextHigh)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Nội dung công việc *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder,
                        focusedLabelColor = LifeOSPrimary,
                        unfocusedLabelColor = LifeOSTextLow,
                        focusedTextColor = LifeOSTextHigh,
                        unfocusedTextColor = LifeOSTextHigh
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("high" to "Cao", "mid" to "TB", "low" to "Thấp").forEach { (pri, label) ->
                        val isSel = priority == pri
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LifeOSPrimary else Color.White.copy(alpha = 0.05f))
                                .clickable { priority = pri }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else LifeOSTextMid
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) onConfirm(text.trim(), priority)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
            ) {
                Text("Thêm ngay", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LifeOSTextMid)
            }
        },
        containerColor = LifeOSSurfaceCard
    )
}
