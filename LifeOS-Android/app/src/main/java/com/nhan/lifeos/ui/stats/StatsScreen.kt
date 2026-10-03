package com.nhan.lifeos.ui.stats

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
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhan.lifeos.core.designsystem.LifeOSAmber
import com.nhan.lifeos.core.designsystem.LifeOSCyan
import com.nhan.lifeos.core.designsystem.LifeOSGreen
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSRed
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceCard
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import com.nhan.lifeos.ui.finance.formatVndShort

@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

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
                        text = "Thống kê & Báo cáo",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = LifeOSTextHigh
                    )
                    Text(
                        text = "Đánh giá hiệu suất & thói quen toàn diện",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextMid
                    )
                }
            }
        }

        // Productivity Score Card
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
                                    LifeOSPrimary.copy(alpha = 0.3f),
                                    Color(0xFF80D8FF).copy(alpha = 0.15f)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ElectricBolt,
                                    contentDescription = null,
                                    tint = LifeOSAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Chỉ số Năng suất LifeOS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LifeOSTextHigh
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when {
                                    uiState.productivityScore >= 80 -> "Xuất sắc! Phong độ rất cao 🔥"
                                    uiState.productivityScore >= 60 -> "Khá tốt, hãy giữ vững nhịp độ!"
                                    else -> "Cần tập trung hoàn thành các mục tiêu"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = LifeOSTextMid
                            )
                        }

                        // Big Score Ring Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(LifeOSPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${uiState.productivityScore}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 4 Key Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsMetricCard(
                    title = "Việc đã xong",
                    value = "${uiState.completedTodos}/${uiState.totalTodos}",
                    sub = "Tỷ lệ ${uiState.todoCompletionRate}%",
                    icon = Icons.Rounded.CheckCircle,
                    color = LifeOSGreen,
                    modifier = Modifier.weight(1f)
                )
                StatsMetricCard(
                    title = "Tỷ lệ tiết kiệm",
                    value = "${uiState.savingsRate}%",
                    sub = "Dòng tiền dương",
                    icon = Icons.Rounded.Savings,
                    color = LifeOSAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsMetricCard(
                    title = "Dự án Kanban",
                    value = "${uiState.doneProjects}/${uiState.totalProjects}",
                    sub = "Cột Hoàn thành",
                    icon = Icons.Rounded.FolderSpecial,
                    color = LifeOSPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatsMetricCard(
                    title = "Mục tiêu dài hạn",
                    value = "${uiState.completedGoals}/${uiState.totalGoals}",
                    sub = "Đã chạm đích",
                    icon = Icons.Rounded.TrackChanges,
                    color = LifeOSCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Task Completion Progress Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tiến độ Công việc (Todos)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LifeOSTextHigh
                        )
                        Text(
                            text = "${uiState.todoCompletionRate}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LifeOSGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (uiState.todoCompletionRate / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = LifeOSGreen,
                        trackColor = Color(0xFF222238)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Còn ${uiState.pendingTodos} công việc đang chờ giải quyết",
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSTextLow
                    )
                }
            }
        }

        // Finance Balance Comparison Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dòng tiền Thu vs Chi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LifeOSTextHigh
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tổng thu: ${formatVndShort(uiState.totalIncome)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LifeOSGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tổng chi: ${formatVndShort(uiState.totalExpense)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = LifeOSRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val totalFlow = (uiState.totalIncome + uiState.totalExpense).coerceAtLeast(1L)
                    LinearProgressIndicator(
                        progress = { (uiState.totalIncome.toFloat() / totalFlow.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = LifeOSGreen,
                        trackColor = LifeOSRed
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Số dư hiện khả dụng: ${formatVndShort(uiState.netBalance)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSTextMid
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StatsMetricCard(
    title: String,
    value: String,
    sub: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = LifeOSTextMid
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextHigh
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
