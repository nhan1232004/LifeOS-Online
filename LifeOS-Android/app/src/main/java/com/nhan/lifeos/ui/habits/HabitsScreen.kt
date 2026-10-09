package com.nhan.lifeos.ui.habits

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.nhan.lifeos.data.local.entity.HabitEntity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }

    // Generate past 7 days (including today)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayOfWeekFmt = SimpleDateFormat("EEE", Locale("vi", "VN"))
    val dayNumFmt = SimpleDateFormat("dd", Locale.getDefault())

    val past7Days = remember {
        val list = mutableListOf<Triple<String, String, String>>() // (isoDate, dayOfWeek, dayNum)
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            list.add(
                Triple(
                    sdf.format(c.time),
                    dayOfWeekFmt.format(c.time),
                    dayNumFmt.format(c.time)
                )
            )
        }
        list
    }

    val todayIso = remember { sdf.format(Calendar.getInstance().time) }
    val todayDoneCount = remember(uiState.habits) {
        uiState.habits.count { h ->
            try {
                val json = JSONObject(h.logsJson)
                json.optBoolean(todayIso, false)
            } catch (e: Exception) { false }
        }
    }
    val todayRate = if (uiState.habits.isNotEmpty()) (todayDoneCount * 100 / uiState.habits.size) else 0
    val maxStreak = remember(uiState.habits) { uiState.habits.maxOfOrNull { it.streak } ?: 0 }
    val activeCount = uiState.habits.size

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
                            text = "Thói quen (Habits)",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = LifeOSTextHigh
                        )
                        Text(
                            text = "Ma trận theo dõi 7 ngày & Chuỗi duy trì",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LifeOSTextMid
                        )
                    }
                }
            }

            // 4 Top KPI Cards (Midnight Aurora Bento)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Hôm nay", style = MaterialTheme.typography.labelSmall, color = LifeOSTextMid)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$todayRate%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LifeOSCyan)
                                Text(text = "$todayDoneCount/$activeCount thói quen", style = MaterialTheme.typography.labelSmall, color = LifeOSTextLow)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Kỷ lục", style = MaterialTheme.typography.labelSmall, color = LifeOSTextMid)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$maxStreak ngày", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LifeOSAmber)
                                Text(text = "Chuỗi dài nhất", style = MaterialTheme.typography.labelSmall, color = LifeOSTextLow)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Bền bỉ", style = MaterialTheme.typography.labelSmall, color = LifeOSTextMid)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "${uiState.totalStreak} ngày", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LifeOSPrimary)
                                Text(text = "Tổng ngày tích lũy", style = MaterialTheme.typography.labelSmall, color = LifeOSTextLow)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = "Đang duy trì", style = MaterialTheme.typography.labelSmall, color = LifeOSTextMid)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$activeCount", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LifeOSGreen)
                                Text(text = "Thói quen hoạt động", style = MaterialTheme.typography.labelSmall, color = LifeOSTextLow)
                            }
                        }
                    }
                }
            }

            // Date Matrix Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Thói quen",
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSTextMid,
                        modifier = Modifier.weight(1.2f)
                    )

                    Row(
                        modifier = Modifier.weight(2f),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        past7Days.forEach { (_, dow, day) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dow,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LifeOSTextLow,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LifeOSTextMid,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Habits List
            if (uiState.habits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có thói quen nào. Nhấn + để bắt đầu xây dựng thói quen!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LifeOSTextLow
                        )
                    }
                }
            } else {
                items(uiState.habits, key = { it.id }) { habit ->
                    HabitCardItem(
                        habit = habit,
                        past7Days = past7Days,
                        onToggleDay = { date -> viewModel.toggleDay(habit.id, date) },
                        onEdit = { editingHabit = habit },
                        onDelete = { viewModel.deleteHabit(habit.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB Add Habit
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = LifeOSPrimary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm thói quen")
        }

        // Add Habit Dialog
        if (showAddDialog) {
            AddHabitDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, target ->
                    viewModel.addHabit(name, target)
                    showAddDialog = false
                }
            )
        }

        // Edit Habit Dialog
        editingHabit?.let { habit ->
            EditHabitDialog(
                habit = habit,
                onDismiss = { editingHabit = null },
                onSave = { updated ->
                    viewModel.updateHabit(updated)
                    editingHabit = null
                },
                onDelete = {
                    viewModel.deleteHabit(habit.id)
                    editingHabit = null
                }
            )
        }
    }
}

@Composable
fun HabitCardItem(
    habit: HabitEntity,
    past7Days: List<Triple<String, String, String>>,
    onToggleDay: (String) -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit
) {
    val logs = remember(habit.logsJson) {
        try { JSONObject(habit.logsJson) } catch (e: Exception) { JSONObject() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onEdit() }
                ) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LifeOSTextHigh
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = habit.target,
                            style = MaterialTheme.typography.labelSmall,
                            color = LifeOSTextMid
                        )
                        if (habit.streak > 0) {
                            Text(
                                text = "🔥 ${habit.streak} ngày",
                                style = MaterialTheme.typography.labelSmall,
                                color = LifeOSAmber,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Sửa",
                            tint = LifeOSTextLow,
                            modifier = Modifier.size(18.dp)
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

            Spacer(modifier = Modifier.height(12.dp))

            // 7 Days Checkboxes Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                past7Days.forEach { (isoDate, _, _) ->
                    val isChecked = logs.optBoolean(isoDate, false)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isChecked) LifeOSPrimary else Color(0xFF131A36)
                            )
                            .clickable { onToggleDay(isoDate) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddHabitDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, target: String) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("Mỗi ngày") }

    val targetOptions = listOf("Mỗi ngày", "5 ngày/tuần", "3 ngày/tuần", "Cuối tuần")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Thêm thói quen mới",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextHigh
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Tên thói quen (VD: Uống 2L nước)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSGreen,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                Text(
                    text = "Mục tiêu thực hiện:",
                    style = MaterialTheme.typography.labelSmall,
                    color = LifeOSTextMid
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(targetOptions) { target ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (targetText == target) LifeOSGreen.copy(alpha = 0.2f)
                                    else LifeOSSurfaceCard
                                )
                                .clickable { targetText = target }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = target,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (targetText == target) LifeOSGreen else LifeOSTextMid,
                                fontWeight = if (targetText == target) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onAdd(nameText, targetText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSGreen)
            ) {
                Text("Bắt đầu thói quen", color = Color.Black, fontWeight = FontWeight.Bold)
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
fun EditHabitDialog(
    habit: HabitEntity,
    onDismiss: () -> Unit,
    onSave: (HabitEntity) -> Unit,
    onDelete: () -> Unit
) {
    var nameText by remember { mutableStateOf(habit.name) }
    var targetText by remember { mutableStateOf(habit.target) }
    val targetOptions = listOf("Mỗi ngày", "5 ngày/tuần", "3 ngày/tuần", "Cuối tuần", "2 lần/ngày")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chỉnh sửa thói quen",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextHigh
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Tên thói quen *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSGreen,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                Text(
                    text = "Mục tiêu thực hiện:",
                    style = MaterialTheme.typography.labelSmall,
                    color = LifeOSTextMid
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(targetOptions) { target ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (targetText == target) LifeOSGreen.copy(alpha = 0.2f)
                                    else LifeOSSurfaceCard
                                )
                                .clickable { targetText = target }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = target,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (targetText == target) LifeOSGreen else LifeOSTextMid,
                                fontWeight = if (targetText == target) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onSave(habit.copy(name = nameText.trim(), target = targetText.trim()))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSGreen)
            ) {
                Text("Lưu thay đổi", color = Color.Black, fontWeight = FontWeight.Bold)
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
        containerColor = Color(0xFF141424),
        shape = RoundedCornerShape(20.dp)
    )
}
