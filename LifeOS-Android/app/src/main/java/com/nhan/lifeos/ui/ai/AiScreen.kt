package com.nhan.lifeos.ui.ai

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

@Composable
fun AiScreen(
    viewModel: AiViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showKeyDialog by remember { mutableStateOf(false) }

    val suggestions = listOf(
        "🎯 Kế hoạch hôm nay",
        "📊 Đánh giá năng suất tuần",
        "💰 Báo cáo tài chính & thu chi",
        "🌿 Chuỗi thói quen",
        "📁 Tiến độ các dự án",
        "💡 Lời khuyên phát triển bản thân"
    )

    // Auto-scroll to bottom on new message
    LaunchedEffect(uiState.messages.size, uiState.isThinking) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Top App Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Trợ lý AI LifeOS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = LifeOSTextHigh
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = LifeOSCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = if (uiState.apiKey.isNotBlank()) "Gemini Flash • Trực tuyến" else "Cố vấn cá nhân nội bộ",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.apiKey.isNotBlank()) LifeOSGreen else LifeOSTextMid
                    )
                }
            }

            // Key setup button / badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (uiState.apiKey.isNotBlank()) LifeOSGreen.copy(alpha = 0.15f)
                        else LifeOSAmber.copy(alpha = 0.15f)
                    )
                    .clickable { showKeyDialog = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = null,
                        tint = if (uiState.apiKey.isNotBlank()) LifeOSGreen else LifeOSAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (uiState.apiKey.isNotBlank()) "Đã kết nối" else "Cài đặt Key",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.apiKey.isNotBlank()) LifeOSGreen else LifeOSAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.messages, key = { it.id }) { msg ->
                AiMessageBubble(message = msg)
            }

            if (uiState.isThinking) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
                    ) {
                        CircularProgressIndicator(
                            color = LifeOSCyan,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "LifeOS AI đang suy nghĩ & phân tích...",
                            style = MaterialTheme.typography.labelSmall,
                            color = LifeOSCyan
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Suggestion Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(suggestions) { sug ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LifeOSSurfaceCard)
                        .clickable { viewModel.sendMessage(sug) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = sug,
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        "Hỏi LifeOS AI về công việc, tiền bạc...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextLow
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LifeOSSurfaceCard,
                    unfocusedContainerColor = LifeOSSurfaceCard,
                    focusedBorderColor = LifeOSCyan,
                    unfocusedBorderColor = LifeOSGlassBorder,
                    focusedTextColor = LifeOSTextHigh,
                    unfocusedTextColor = LifeOSTextHigh
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LifeOSCyan)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Gửi",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    // Gemini API Key Config Dialog
    if (showKeyDialog) {
        GeminiKeyConfigDialog(
            currentKey = uiState.apiKey,
            currentModel = uiState.model,
            isTesting = uiState.isTestingKey,
            testResult = uiState.testKeyResult,
            isKeyValid = uiState.isKeyValid,
            onTest = { k, m -> viewModel.testGeminiKey(k, m) },
            onSave = { k, m ->
                viewModel.saveGeminiConfig(k, m)
                showKeyDialog = false
            },
            onDismiss = {
                viewModel.clearTestStatus()
                showKeyDialog = false
            }
        )
    }
}

@Composable
fun GeminiKeyConfigDialog(
    currentKey: String,
    currentModel: String,
    isTesting: Boolean,
    testResult: String?,
    isKeyValid: Boolean?,
    onTest: (String, String) -> Unit,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var keyText by remember { mutableStateOf(currentKey) }
    var selectedModel by remember { mutableStateOf(currentModel) }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    tint = LifeOSCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cài đặt Gemini AI",
                    fontWeight = FontWeight.Bold,
                    color = LifeOSTextHigh
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Kích hoạt mô hình Google Gemini để AI có thể hiểu sâu sắc dữ liệu cá nhân của bạn, lên kế hoạch thông minh và sáng tạo tự do.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LifeOSTextMid,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Key Input
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("Dán mã bắt đầu bằng AIzaSy...") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = LifeOSTextMid,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        keyText = clip.trim()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "Dán",
                                    tint = LifeOSCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Model Selection Chips
                Text(
                    text = "Mô hình ngôn ngữ:",
                    style = MaterialTheme.typography.labelMedium,
                    color = LifeOSTextHigh,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedModel == "gemini-flash-latest",
                        onClick = { selectedModel = "gemini-flash-latest" },
                        label = { Text("Flash mới nhất") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LifeOSCyan.copy(alpha = 0.2f),
                            selectedLabelColor = LifeOSCyan
                        )
                    )
                    FilterChip(
                        selected = selectedModel == "gemini-3.8-flash",
                        onClick = { selectedModel = "gemini-3.8-flash" },
                        label = { Text("3.8 Flash") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LifeOSCyan.copy(alpha = 0.2f),
                            selectedLabelColor = LifeOSCyan
                        )
                    )
                    FilterChip(
                        selected = selectedModel == "gemini-3.5-flash",
                        onClick = { selectedModel = "gemini-3.5-flash" },
                        label = { Text("3.5 Flash") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LifeOSCyan.copy(alpha = 0.2f),
                            selectedLabelColor = LifeOSCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test Connection Button & Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { onTest(keyText, selectedModel) },
                        enabled = !isTesting && keyText.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = LifeOSCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đang kiểm tra...", fontSize = 12.sp)
                        } else {
                            Text("Kiểm tra kết nối", fontSize = 12.sp, color = LifeOSCyan)
                        }
                    }

                    if (keyText.isNotBlank()) {
                        TextButton(
                            onClick = { keyText = "" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Xóa Key", color = LifeOSRed, fontSize = 12.sp)
                        }
                    }
                }

                // Test Result Feedback
                if (testResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isKeyValid == true) LifeOSGreen.copy(alpha = 0.12f)
                                else LifeOSRed.copy(alpha = 0.12f)
                            )
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = if (isKeyValid == true) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = if (isKeyValid == true) LifeOSGreen else LifeOSRed,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = testResult,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isKeyValid == true) LifeOSGreen else LifeOSRed,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🔑 Lấy mã miễn phí tại: aistudio.google.com/app/apikey",
                    style = MaterialTheme.typography.labelSmall,
                    color = LifeOSTextLow,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyText, selectedModel) },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Lưu cấu hình", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = LifeOSTextMid)
            }
        }
    )
}

@Composable
fun AiMessageBubble(message: AiMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(LifeOSCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = LifeOSCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Card(
            modifier = Modifier.widthIn(max = 310.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) LifeOSPrimary else LifeOSSurfaceCard
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else LifeOSTextHigh,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUser) Color.White.copy(alpha = 0.6f) else LifeOSTextLow,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
