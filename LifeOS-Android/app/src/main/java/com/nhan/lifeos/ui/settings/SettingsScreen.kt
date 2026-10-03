package com.nhan.lifeos.ui.settings

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nhan.lifeos.core.designsystem.LifeOSAmber
import com.nhan.lifeos.core.designsystem.LifeOSCyan
import com.nhan.lifeos.core.designsystem.LifeOSGreen
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSRed
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceCard
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import com.nhan.lifeos.data.preferences.UserSession
import com.nhan.lifeos.data.repository.CloudSyncRepository
import com.nhan.lifeos.data.repository.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    cloudSyncRepo: CloudSyncRepository? = null,
    userSession: UserSession = UserSession(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncState by (cloudSyncRepo?.syncState ?: remember { MutableStateFlow(SyncState.Idle) }).collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

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
                        text = "Cài đặt & Dữ liệu",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = LifeOSTextHigh
                    )
                    Text(
                        text = "Sao lưu dữ liệu JSON & Quản lý bộ nhớ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextMid
                    )
                }
            }
        }

        // Cloud Sync Card (Firestore Live Sync)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSCyan.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LifeOSCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudSync,
                                contentDescription = null,
                                tint = LifeOSCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Đồng bộ Đám mây (Web & App)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LifeOSTextHigh
                            )
                            Text(
                                text = if (userSession.canSyncOnline) "Tài khoản: ${userSession.userEmail}" else "Chế độ Khách (Offline)",
                                style = MaterialTheme.typography.labelSmall,
                                color = LifeOSTextMid
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (userSession.canSyncOnline) {
                        val state = syncState
                        val lastSyncStr = if (userSession.lastSyncedAt > 0L) {
                            val sdf = java.text.SimpleDateFormat("HH:mm - dd/MM/yyyy", java.util.Locale.getDefault())
                            "Lần đồng bộ gần nhất: " + sdf.format(java.util.Date(userSession.lastSyncedAt))
                        } else "Chưa từng đồng bộ dữ liệu"

                        Text(
                            text = when (state) {
                                is SyncState.Syncing -> "Đang kết nối Firestore & đồng bộ toàn bộ dữ liệu..."
                                is SyncState.Success -> "Đồng bộ hoàn tất (${state.totalSynced} mục). Dữ liệu trên App và Web hoàn toàn trùng khớp!"
                                is SyncState.Error -> "Lỗi đồng bộ: ${state.message}"
                                is SyncState.Idle -> "Dữ liệu được đồng bộ 2 chiều tự động qua internet với Web LifeOS (dashboard-39cf8)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (state) {
                                is SyncState.Error -> LifeOSRed
                                is SyncState.Success -> LifeOSGreen
                                else -> LifeOSTextMid
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = lastSyncStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = LifeOSTextLow
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    cloudSyncRepo?.syncAll(userSession)
                                }
                            },
                            enabled = state !is SyncState.Syncing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LifeOSCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state is SyncState.Syncing) {
                                CircularProgressIndicator(
                                    color = Color.Black,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đang đồng bộ...", color = Color.Black, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đồng bộ ngay với Web", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = "Bạn đang ở chế độ Khách Offline. Hãy đăng nhập tài khoản để kích hoạt tính năng đồng bộ tự động 2 chiều qua internet với phiên bản Web trực tuyến.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LifeOSTextLow
                        )
                    }
                }
            }
        }

        // Database Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Storage,
                            contentDescription = null,
                            tint = LifeOSCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Dữ liệu lưu trữ cục bộ (Room DB)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LifeOSTextHigh
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val stats = uiState.stats
                    val items = listOf(
                        "Việc cần làm" to "${stats.todosCount} việc",
                        "Sự kiện lịch" to "${stats.eventsCount} mục",
                        "Dự án Kanban" to "${stats.projectsCount} dự án",
                        "Giao dịch thu chi" to "${stats.transactionsCount} bản ghi",
                        "Mục tiêu dài hạn" to "${stats.goalsCount} mục tiêu",
                        "Ghi chú cá nhân" to "${stats.notesCount} ghi chú",
                        "Thói quen" to "${stats.habitsCount} thói quen",
                        "Nhật ký" to "${stats.journalCount} trang",
                        "Từ vựng Flashcard" to "${stats.vocabCount} thẻ"
                    )

                    items.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "• ${pair[0].first}: ${pair[0].second}",
                                style = MaterialTheme.typography.bodySmall,
                                color = LifeOSTextMid
                            )
                            if (pair.size > 1) {
                                Text(
                                    text = "• ${pair[1].first}: ${pair[1].second}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LifeOSTextMid
                                )
                            }
                        }
                    }
                }
            }
        }

        // Backup Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Backup,
                            contentDescription = null,
                            tint = LifeOSGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sao lưu dữ liệu (Backup)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LifeOSTextHigh
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Xuất toàn bộ cơ sở dữ liệu LifeOS ra file JSON an toàn để lưu trữ trên Google Drive, Zalo hoặc gửi sang thiết bị khác.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LifeOSTextLow
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val jsonStr = viewModel.exportFullBackupJson()
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, jsonStr)
                                    putExtra(Intent.EXTRA_TITLE, "LifeOS_Backup_${System.currentTimeMillis()}.json")
                                    type = "application/json"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Lưu file sao lưu LifeOS"))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LifeOSGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Xuất bản sao lưu JSON",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Reset Data Action Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CleaningServices,
                            contentDescription = null,
                            tint = LifeOSRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Xóa & Đặt lại dữ liệu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LifeOSRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Xóa toàn bộ các bảng trong Room Database để bắt đầu lại từ đầu hoặc thử nghiệm lại dữ liệu mẫu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LifeOSTextLow
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showResetDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LifeOSRed.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Đặt lại toàn bộ dữ liệu",
                            color = LifeOSRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // App Information Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = LifeOSAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Thông tin ứng dụng",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LifeOSTextHigh
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Tên ứng dụng: LifeOS Native Mobile",
                        style = MaterialTheme.typography.bodySmall,
                        color = LifeOSTextMid
                    )
                    Text(
                        text = "• Phiên bản: 1.0.0 (Build Native Release)",
                        style = MaterialTheme.typography.bodySmall,
                        color = LifeOSTextMid
                    )
                    Text(
                        text = "• Công nghệ: Kotlin 2.0 + Jetpack Compose + Room DB",
                        style = MaterialTheme.typography.bodySmall,
                        color = LifeOSTextMid
                    )
                    Text(
                        text = "• Kiến trúc: Clean Architecture + Offline-First",
                        style = MaterialTheme.typography.bodySmall,
                        color = LifeOSTextMid
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Xác nhận đặt lại dữ liệu?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LifeOSRed
                )
            },
            text = {
                Text(
                    text = "Hành động này sẽ xóa toàn bộ các mục công việc, tài chính, ghi chú và lịch sử trên thiết bị. Bạn có chắc chắn muốn thực hiện không?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LifeOSTextMid
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData {
                            showResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOSRed)
                ) {
                    Text("Xóa tất cả", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Hủy", color = LifeOSTextMid)
                }
            },
            containerColor = Color(0xFF141424),
            shape = RoundedCornerShape(20.dp)
        )
    }
}
