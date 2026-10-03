package com.nhan.lifeos.ui.calendar

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhan.lifeos.core.designsystem.LifeOSAmber
import com.nhan.lifeos.core.designsystem.LifeOSCyan
import com.nhan.lifeos.core.designsystem.LifeOSGlassBorder
import com.nhan.lifeos.core.designsystem.LifeOSGreen
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceCard
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import com.nhan.lifeos.data.local.entity.EventEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
    val dayOfWeekFormat = SimpleDateFormat("EEE", Locale("vi", "VN"))

    // Generate next 14 days for date strip
    val dates = remember {
        val list = mutableListOf<Triple<String, String, String>>() // (rawDate, dayNum, dayName)
        val cal = Calendar.getInstance()
        for (i in 0..13) {
            val d = cal.time
            list.add(
                Triple(
                    sdf.format(d),
                    dayFormat.format(d),
                    dayOfWeekFormat.format(d).replaceFirstChar { it.uppercase() }
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Lịch & Sự kiện",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = LifeOSTextHigh
            )
            Text(
                text = "Lịch trình công việc và thời gian biểu chi tiết",
                style = MaterialTheme.typography.bodyMedium,
                color = LifeOSTextMid,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Horizontal Day Selector Strip
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dates) { (rawDate, dayNum, dayName) ->
                    val isSelected = uiState.selectedDate == rawDate
                    Card(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.selectDate(rawDate) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) LifeOSPrimary else LifeOSSurfaceCard
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else LifeOSTextLow
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dayNum,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else LifeOSTextHigh
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Sự kiện ngày ${uiState.selectedDate} (${uiState.eventsForSelectedDate.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextHigh
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.eventsForSelectedDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không có sự kiện nào trong ngày này",
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
                    items(uiState.eventsForSelectedDate, key = { it.id }) { event ->
                        CalendarEventCard(
                            event = event,
                            onDelete = { viewModel.deleteEvent(event.id) }
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
            Icon(Icons.Rounded.Add, contentDescription = "Thêm sự kiện")
        }
    }

    if (showAddDialog) {
        AddEventDialog(
            initialDate = uiState.selectedDate,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, dateStart, timeStart, timeEnd, type, desc ->
                viewModel.addEvent(title, dateStart, timeStart, timeEnd, type, desc)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CalendarEventCard(
    event: EventEntity,
    onDelete: () -> Unit
) {
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
                    .size(4.dp, 40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(typeColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = LifeOSTextHigh
                )
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LifeOSTextMid
                )
                if (event.desc.isNotBlank()) {
                    Text(
                        text = event.desc,
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSTextLow,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Text(
                text = event.type.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = typeColor,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Xóa",
                    tint = LifeOSTextLow,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun AddEventDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dateStart: String, timeStart: String, timeEnd: String, type: String, desc: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var dateStart by remember { mutableStateOf(initialDate) }
    var timeStart by remember { mutableStateOf("") }
    var timeEnd by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("work") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Thêm sự kiện mới", fontWeight = FontWeight.Bold, color = LifeOSTextHigh)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề sự kiện *") },
                    colors = customEventDialogColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateStart,
                    onValueChange = { dateStart = it },
                    label = { Text("Ngày (YYYY-MM-DD)") },
                    colors = customEventDialogColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = timeStart,
                        onValueChange = { timeStart = it },
                        label = { Text("Giờ bắt đầu") },
                        placeholder = { Text("09:00") },
                        colors = customEventDialogColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = timeEnd,
                        onValueChange = { timeEnd = it },
                        label = { Text("Giờ kết thúc") },
                        placeholder = { Text("10:30") },
                        colors = customEventDialogColors(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("work" to "Việc", "study" to "Học", "health" to "Khỏe").forEach { (tp, label) ->
                        val isSel = type == tp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LifeOSPrimary else Color.White.copy(alpha = 0.05f))
                                .clickable { type = tp }
                                .padding(vertical = 7.dp),
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

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Mô tả sự kiện") },
                    colors = customEventDialogColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onConfirm(title.trim(), dateStart.trim(), timeStart.trim(), timeEnd.trim(), type, desc.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
            ) {
                Text("Lưu sự kiện", fontWeight = FontWeight.Bold)
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
private fun customEventDialogColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LifeOSPrimary,
    unfocusedBorderColor = LifeOSGlassBorder,
    focusedLabelColor = LifeOSPrimary,
    unfocusedLabelColor = LifeOSTextLow,
    focusedTextColor = LifeOSTextHigh,
    unfocusedTextColor = LifeOSTextHigh
)
