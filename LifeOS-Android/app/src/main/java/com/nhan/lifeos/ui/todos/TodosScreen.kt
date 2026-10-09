package com.nhan.lifeos.ui.todos

import androidx.compose.foundation.background
import com.nhan.lifeos.ui.common.LifeOSDateField
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.nhan.lifeos.data.local.entity.TodoEntity

@Composable
fun TodosScreen(viewModel: TodosViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<TodoEntity?>(null) }

    val filterTabs = listOf(
        TodoFilter.ALL to "Tất cả (${uiState.todos.size})",
        TodoFilter.PENDING to "Chưa xong (${uiState.pendingCount})",
        TodoFilter.COMPLETED to "Đã xong (${uiState.completedCount})"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Việc cần làm",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = LifeOSTextHigh
            )
            Text(
                text = "Quản lý và theo dõi tiến độ nhiệm vụ",
                style = MaterialTheme.typography.bodyMedium,
                color = LifeOSTextMid,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Filter Tabs
            TabRow(
                selectedTabIndex = uiState.currentFilter.ordinal,
                containerColor = Color.Transparent,
                contentColor = LifeOSPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.currentFilter.ordinal]),
                        color = LifeOSPrimary
                    )
                }
            ) {
                filterTabs.forEach { (filter, title) ->
                    val isSelected = uiState.currentFilter == filter
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(filter) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) LifeOSPrimary else LifeOSTextMid
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Todos List
            if (uiState.todos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không có công việc nào trong danh sách",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LifeOSTextLow
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.todos, key = { it.id }) { todo ->
                        TodoCardItem(
                            todo = todo,
                            onToggle = { viewModel.toggleTodo(todo.id, !todo.done) },
                            onEdit = { editingTodo = todo },
                            onDelete = { viewModel.deleteTodo(todo.id) }
                        )
                    }
                }
            }
        }

        // Add FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = LifeOSPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Thêm việc cần làm")
        }
    }

    if (showAddDialog) {
        AddTodoDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { text, priority, date, note ->
                viewModel.addTodo(text, priority, date, note)
                showAddDialog = false
            }
        )
    }

    editingTodo?.let { todo ->
        EditTodoDialog(
            todo = todo,
            onDismiss = { editingTodo = null },
            onSave = { updated ->
                viewModel.updateTodo(updated)
                editingTodo = null
            },
            onDelete = {
                viewModel.deleteTodo(todo.id)
                editingTodo = null
            }
        )
    }
}

@Composable
private fun TodoCardItem(
    todo: TodoEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priColor = when (todo.priority.lowercase()) {
        "high" -> LifeOSRed
        "mid" -> LifeOSAmber
        else -> LifeOSCyan
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEdit() }
            ) {
                Text(
                    text = todo.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (todo.done) LifeOSTextLow else LifeOSTextHigh,
                    textDecoration = if (todo.done) TextDecoration.LineThrough else TextDecoration.None
                )
                if (todo.note.isNotBlank() || todo.date.isNotBlank()) {
                    Row(modifier = Modifier.padding(top = 2.dp)) {
                        if (todo.date.isNotBlank()) {
                            Text(
                                text = "Hạn: ${todo.date}  ",
                                style = MaterialTheme.typography.labelSmall,
                                color = LifeOSTextLow
                            )
                        }
                        if (todo.note.isNotBlank()) {
                            Text(
                                text = "• ${todo.note}",
                                style = MaterialTheme.typography.labelSmall,
                                color = LifeOSTextLow
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(priColor.copy(alpha = 0.15f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = todo.priority.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = priColor,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Chỉnh sửa",
                    tint = LifeOSTextLow,
                    modifier = Modifier.size(17.dp)
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Xóa",
                    tint = LifeOSTextLow,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, priority: String, date: String, note: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("high") }
    var date by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Thêm việc cần làm", fontWeight = FontWeight.Bold, color = LifeOSTextHigh)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Nội dung công việc *") },
                    colors = customTodoDialogColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority Selector
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

                LifeOSDateField(
                    value = date,
                    onValueChange = { date = it },
                    label = "Ngày thực hiện"
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú bổ sung") },
                    colors = customTodoDialogColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) onConfirm(text.trim(), priority, date.trim(), note.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
            ) {
                Text("Lưu công việc", fontWeight = FontWeight.Bold)
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

@Composable
private fun EditTodoDialog(
    todo: TodoEntity,
    onDismiss: () -> Unit,
    onSave: (TodoEntity) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember { mutableStateOf(todo.text) }
    var priority by remember { mutableStateOf(todo.priority) }
    var date by remember { mutableStateOf(todo.date) }
    var note by remember { mutableStateOf(todo.note) }
    var done by remember { mutableStateOf(todo.done) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Chỉnh sửa việc cần làm", fontWeight = FontWeight.Bold, color = LifeOSTextHigh)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Nội dung công việc *") },
                    colors = customTodoDialogColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("high" to "Cao", "mid" to "TB", "low" to "Thấp").forEach { (pri, label) ->
                        val isSel = priority.lowercase() == pri
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

                LifeOSDateField(
                    value = date,
                    onValueChange = { date = it },
                    label = "Ngày thực hiện"
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú bổ sung") },
                    colors = customTodoDialogColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { done = !done }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = done,
                        onCheckedChange = { done = it },
                        colors = CheckboxDefaults.colors(checkedColor = LifeOSPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (done) "Đã hoàn thành" else "Chưa hoàn thành",
                        color = LifeOSTextHigh,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(
                            todo.copy(
                                text = text.trim(),
                                priority = priority,
                                date = date.trim(),
                                note = note.trim(),
                                done = done
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
            ) {
                Text("Lưu thay đổi", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Xóa", color = LifeOSRed)
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy", color = LifeOSTextMid)
                }
            }
        },
        containerColor = LifeOSSurfaceCard
    )
}

@Composable
private fun customTodoDialogColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LifeOSPrimary,
    unfocusedBorderColor = LifeOSGlassBorder,
    focusedLabelColor = LifeOSPrimary,
    unfocusedLabelColor = LifeOSTextLow,
    focusedTextColor = LifeOSTextHigh,
    unfocusedTextColor = LifeOSTextHigh
)
