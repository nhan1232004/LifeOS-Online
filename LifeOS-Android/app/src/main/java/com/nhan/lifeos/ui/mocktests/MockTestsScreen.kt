package com.nhan.lifeos.ui.mocktests

import androidx.compose.foundation.background
import com.nhan.lifeos.ui.common.LifeOSDateField
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.School
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MockTestsScreen(
    viewModel: MockTestsViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = LifeOSPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Nhập điểm thi")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                            text = "Luyện thi (IELTS / TOEIC)",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = LifeOSTextHigh
                        )
                        Text(
                            text = "Theo dõi điểm 4 kỹ năng & tiến độ ôn luyện",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LifeOSTextMid
                        )
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSGlassBorder)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(LifeOSCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EmojiEvents,
                                        contentDescription = null,
                                        tint = LifeOSCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Tổng quan kết quả",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = LifeOSTextHigh
                                    )
                                    Text(
                                        text = "${uiState.totalCount} bài thi đã lưu",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LifeOSTextMid
                                    )
                                }
                            }

                            if (uiState.totalCount > 0) {
                                Text(
                                    text = "Avg: ${uiState.avgScore}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LifeOSCyan
                                )
                            }
                        }

                        if (uiState.totalCount > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(LifeOSSurfaceDark)
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                SkillStatItem(label = "Nghe", score = uiState.avgList.toString(), color = LifeOSGreen)
                                SkillStatItem(label = "Đọc", score = uiState.avgRead.toString(), color = LifeOSAmber)
                                SkillStatItem(label = "Nói", score = uiState.avgSpeak.toString(), color = Color(0xFFFF80AB))
                                SkillStatItem(label = "Viết", score = uiState.avgWrite.toString(), color = LifeOSCyan)
                            }
                        }
                    }
                }
            }

            // List of Mock Tests
            if (uiState.tests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = null,
                                tint = LifeOSTextLow,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Chưa có kết quả thi thử nào",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LifeOSTextHigh
                            )
                            Text(
                                text = "Lưu điểm thi 4 kỹ năng (Nghe, Đọc, Nói, Viết) để theo dõi biểu đồ tiến bộ theo thời gian và đồng bộ với bản Web.",
                                style = MaterialTheme.typography.bodySmall,
                                color = LifeOSTextMid,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )
                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
                            ) {
                                Text("Nhập điểm bài thi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(uiState.tests, key = { it.id }) { test ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSGlassBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = test.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = LifeOSTextHigh
                                    )
                                    Text(
                                        text = test.date,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LifeOSTextMid
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteMockTest(test.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Xóa bài thi",
                                        tint = LifeOSRed.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LifeOSSurfaceDark)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                SkillBadge("Nghe", test.list.toString(), LifeOSGreen)
                                SkillBadge("Đọc", test.read.toString(), LifeOSAmber)
                                SkillBadge("Nói", test.speak.toString(), Color(0xFFFF80AB))
                                SkillBadge("Viết", test.write.toString(), LifeOSCyan)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (test.note.isNotBlank()) test.note else "Không có ghi chú",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LifeOSTextLow,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Tổng: ${test.total}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LifeOSPrimary
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddDialog) {
        AddMockTestDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, date, list, read, speak, write, note ->
                viewModel.addMockTest(name, date, list, read, speak, write, note)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SkillStatItem(label: String, score: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = LifeOSTextLow)
        Text(text = score, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun SkillBadge(label: String, score: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = LifeOSTextLow, fontSize = 10.sp)
        Text(text = score, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun AddMockTestDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, date: String, list: Double, read: Double, speak: Double, write: Double, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var date by remember { mutableStateOf(todayStr) }
    var listScore by remember { mutableStateOf("") }
    var readScore by remember { mutableStateOf("") }
    var speakScore by remember { mutableStateOf("") }
    var writeScore by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Nhập Điểm Thi Thử", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên bài thi (VD: IELTS Cam 18 Test 1)") },
                    singleLine = true,
                    colors = customDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                LifeOSDateField(
                    value = date,
                    onValueChange = { date = it },
                    label = "Ngày thi"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = listScore,
                        onValueChange = { listScore = it },
                        label = { Text("Nghe") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = customDialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = readScore,
                        onValueChange = { readScore = it },
                        label = { Text("Đọc") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = customDialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = speakScore,
                        onValueChange = { speakScore = it },
                        label = { Text("Nói") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = customDialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = writeScore,
                        onValueChange = { writeScore = it },
                        label = { Text("Viết") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = customDialogTextFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Ghi chú (tùy chọn)") },
                    singleLine = true,
                    colors = customDialogTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(text = it, color = LifeOSRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Vui lòng nhập tên bài thi"
                        return@Button
                    }
                    val l = listScore.toDoubleOrNull() ?: 0.0
                    val r = readScore.toDoubleOrNull() ?: 0.0
                    val s = speakScore.toDoubleOrNull() ?: 0.0
                    val w = writeScore.toDoubleOrNull() ?: 0.0
                    onAdd(name.trim(), date.trim(), l, r, s, w, note.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
            ) {
                Text("Lưu kết quả", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LifeOSTextMid)
            }
        },
        containerColor = LifeOSSurfaceCard,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun customDialogTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LifeOSPrimary,
    unfocusedBorderColor = LifeOSGlassBorder,
    focusedLabelColor = LifeOSPrimary,
    unfocusedLabelColor = LifeOSTextLow,
    focusedTextColor = LifeOSTextHigh,
    unfocusedTextColor = LifeOSTextHigh
)
