package com.nhan.lifeos.ui.calendar

import androidx.compose.foundation.background
import com.nhan.lifeos.ui.common.LifeOSDateField
import com.nhan.lifeos.ui.common.LifeOSTimeField
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditCalendar
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhan.lifeos.core.designsystem.LifeOSAmber
import com.nhan.lifeos.core.designsystem.LifeOSCyan
import com.nhan.lifeos.core.designsystem.LifeOSGlassBorder
import com.nhan.lifeos.core.designsystem.LifeOSGreen
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSRed
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceCard
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceDark
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import com.nhan.lifeos.data.local.entity.EventEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
    val dayOfWeekFormat = SimpleDateFormat("EEE", Locale("vi", "VN"))

    // Generate 14 days for week view
    val weekDates = remember {
        val list = mutableListOf<Triple<String, String, String>>()
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

            // 1. Header & View Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Lịch & Sự kiện",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = LifeOSTextHigh
                    )
                    Text(
                        text = "Quản lý thời gian biểu và cuộc hẹn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextMid
                    )
                }

                // View Mode Pill (Tháng vs Tuần)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(LifeOSSurfaceDark)
                        .border(1.dp, LifeOSGlassBorder, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (uiState.viewMode == "month") LifeOSPrimary else Color.Transparent)
                            .clickable { viewModel.setViewMode("month") }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Tháng",
                            fontSize = 12.sp,
                            fontWeight = if (uiState.viewMode == "month") FontWeight.Bold else FontWeight.Medium,
                            color = if (uiState.viewMode == "month") Color.White else LifeOSTextMid
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (uiState.viewMode == "week") LifeOSPrimary else Color.Transparent)
                            .clickable { viewModel.setViewMode("week") }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Tuần",
                            fontSize = 12.sp,
                            fontWeight = if (uiState.viewMode == "week") FontWeight.Bold else FontWeight.Medium,
                            color = if (uiState.viewMode == "week") Color.White else LifeOSTextMid
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Calendar View (Month vs Week)
            if (uiState.viewMode == "month") {
                // Month Navigation Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSGlassBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.previousMonth() }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                        contentDescription = "Tháng trước",
                                        tint = LifeOSTextHigh
                                    )
                                }
                                Text(
                                    text = uiState.monthTitle,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = LifeOSTextHigh,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(onClick = { viewModel.nextMonth() }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = "Tháng sau",
                                        tint = LifeOSTextHigh
                                    )
                                }
                            }

                            TextButton(
                                onClick = { viewModel.goToToday() },
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                            ) {
                                Text("Hôm nay", fontSize = 12.sp, color = LifeOSPrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Day of week labels
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { dayLabel ->
                                Text(
                                    text = dayLabel,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (dayLabel == "CN") LifeOSRed else LifeOSTextLow
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 7-Column Month Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            userScrollEnabled = false
                        ) {
                            items(uiState.monthDays, key = { it.dateString + it.dayNumber }) { day ->
                                val isSelected = day.dateString == uiState.selectedDate
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1.2f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected -> LifeOSPrimary
                                                day.isToday -> LifeOSPrimary.copy(alpha = 0.2f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { viewModel.selectDate(day.dateString) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = day.dayNumber.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White
                                                day.isToday -> LifeOSCyan
                                                day.isCurrentMonth -> LifeOSTextHigh
                                                else -> LifeOSTextLow.copy(alpha = 0.4f)
                                            }
                                        )
                                        if (day.eventCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else LifeOSCyan)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 14-Day Horizontal Strip
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(weekDates) { (dateStr, dayNum, dayName) ->
                        val isSelected = dateStr == uiState.selectedDate
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) LifeOSPrimary else LifeOSSurfaceDark)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) LifeOSPrimary else LifeOSGlassBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.selectDate(dateStr) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else LifeOSTextLow
                                )
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
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Selected Date Header & Event Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sự kiện ngày ${uiState.selectedDate}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LifeOSTextHigh
                )
                Text(
                    text = "${uiState.eventsForSelectedDate.size} sự kiện",
                    style = MaterialTheme.typography.labelSmall,
                    color = LifeOSCyan
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Events List for Selected Date
            if (uiState.eventsForSelectedDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Không có sự kiện nào trong ngày này",
                            style = MaterialTheme.typography.bodyLarge,
                            color = LifeOSTextLow
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = { showAddDialog = true }) {
                            Text("+ Thêm sự kiện mới", color = LifeOSPrimary)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.eventsForSelectedDate, key = { it.id }) { event ->
                        EventCardItem(
                            event = event,
                            onClick = { editingEvent = event },
                            onDelete = { viewModel.deleteEvent(event.id) }
                        )
                    }
                }
            }
        }

        // Add Event FAB
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

    // Add Event Dialog
    if (showAddDialog) {
        EventFormDialog(
            title = "Thêm sự kiện mới",
            initialTitle = "",
            initialDateStart = uiState.selectedDate,
            initialDateEnd = uiState.selectedDate,
            initialTimeStart = "",
            initialTimeEnd = "",
            initialType = "work",
            initialDesc = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { t, ds, de, ts, te, type, desc ->
                viewModel.addEvent(
                    title = t,
                    dateStart = ds,
                    dateEnd = de,
                    timeStart = ts,
                    timeEnd = te,
                    type = type,
                    desc = desc
                )
                showAddDialog = false
            }
        )
    }

    // Edit Event Dialog (Điều chỉnh lịch)
    if (editingEvent != null) {
        val ev = editingEvent!!
        EventFormDialog(
            title = "Điều chỉnh sự kiện",
            initialTitle = ev.title,
            initialDateStart = ev.dateStart,
            initialDateEnd = ev.dateEnd,
            initialTimeStart = ev.timeStart,
            initialTimeEnd = ev.timeEnd,
            initialType = ev.type,
            initialDesc = ev.desc,
            isEditing = true,
            onDelete = {
                viewModel.deleteEvent(ev.id)
                editingEvent = null
            },
            onDismiss = { editingEvent = null },
            onConfirm = { t, ds, de, ts, te, type, desc ->
                viewModel.updateEvent(
                    ev.copy(
                        title = t,
                        dateStart = ds,
                        dateEnd = de,
                        timeStart = ts,
                        timeEnd = te,
                        type = type,
                        desc = desc
                    )
                )
                editingEvent = null
            }
        )
    }
}

@Composable
private fun EventCardItem(
    event: EventEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val typeColor = when (event.type.lowercase()) {
        "work", "công việc" -> LifeOSPrimary
        "personal", "cá nhân" -> LifeOSCyan
        "study", "học tập" -> LifeOSGreen
        else -> LifeOSAmber
    }

    val typeLabel = when (event.type.lowercase()) {
        "work", "công việc" -> "Công việc"
        "personal", "cá nhân" -> "Cá nhân"
        "study", "học tập" -> "Học tập"
        else -> "Quan trọng"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSGlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(typeColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LifeOSTextHigh
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (event.timeStart.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = LifeOSTextLow,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (event.timeEnd.isNotBlank()) "${event.timeStart} - ${event.timeEnd}" else event.timeStart,
                                style = MaterialTheme.typography.labelSmall,
                                color = LifeOSTextMid
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                }

                if (event.desc.isNotBlank()) {
                    Text(
                        text = event.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = LifeOSTextLow,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Xóa sự kiện",
                    tint = LifeOSTextLow,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EventFormDialog(
    title: String,
    initialTitle: String,
    initialDateStart: String,
    initialDateEnd: String,
    initialTimeStart: String,
    initialTimeEnd: String,
    initialType: String,
    initialDesc: String,
    isEditing: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, dateStart: String, dateEnd: String, timeStart: String, timeEnd: String, type: String, desc: String) -> Unit
) {
    var titleVal by remember { mutableStateOf(initialTitle) }
    var dateStartVal by remember { mutableStateOf(initialDateStart) }
    var dateEndVal by remember { mutableStateOf(initialDateEnd) }
    var timeStartVal by remember { mutableStateOf(initialTimeStart) }
    var timeEndVal by remember { mutableStateOf(initialTimeEnd) }
    var typeVal by remember { mutableStateOf(initialType) }
    var descVal by remember { mutableStateOf(initialDesc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, color = LifeOSTextHigh)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titleVal,
                    onValueChange = { titleVal = it },
                    label = { Text("Tên sự kiện *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LifeOSDateField(
                        value = dateStartVal,
                        onValueChange = { dateStartVal = it },
                        label = "Ngày bắt đầu",
                        modifier = Modifier.weight(1f)
                    )
                    LifeOSDateField(
                        value = dateEndVal,
                        onValueChange = { dateEndVal = it },
                        label = "Ngày kết thúc",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LifeOSTimeField(
                        value = timeStartVal,
                        onValueChange = { timeStartVal = it },
                        label = "Giờ bắt đầu",
                        modifier = Modifier.weight(1f)
                    )
                    LifeOSTimeField(
                        value = timeEndVal,
                        onValueChange = { timeEndVal = it },
                        label = "Giờ kết thúc",
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Phân loại:", style = MaterialTheme.typography.labelSmall, color = LifeOSTextLow)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("work" to "Công việc", "personal" to "Cá nhân", "study" to "Học tập").forEach { (typeKey, typeLabel) ->
                        val isSel = typeVal == typeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LifeOSPrimary else LifeOSSurfaceDark)
                                .clickable { typeVal = typeKey }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = typeLabel,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else LifeOSTextMid
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = descVal,
                    onValueChange = { descVal = it },
                    label = { Text("Ghi chú / Địa điểm") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Xóa", color = LifeOSRed)
                    }
                }
                Button(
                    onClick = {
                        if (titleVal.isNotBlank() && dateStartVal.isNotBlank()) {
                            onConfirm(
                                titleVal.trim(),
                                dateStartVal.trim(),
                                dateEndVal.ifBlank { dateStartVal }.trim(),
                                timeStartVal.trim(),
                                timeEndVal.trim(),
                                typeVal,
                                descVal.trim()
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
                ) {
                    Text(if (isEditing) "Lưu thay đổi" else "Thêm sự kiện")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LifeOSTextMid)
            }
        }
    )
}
