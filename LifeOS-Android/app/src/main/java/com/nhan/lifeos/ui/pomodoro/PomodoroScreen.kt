package com.nhan.lifeos.ui.pomodoro

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedPresetIndex by remember { mutableIntStateOf(0) }

    val presets = listOf(
        Triple("Tập trung", 25, true),
        Triple("Nghỉ ngắn", 5, false),
        Triple("Nghỉ dài", 15, false)
    )

    val activeColor = if (uiState.isWorkMode) LifeOSPrimary else LifeOSCyan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
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
                    text = "Pomodoro Timer",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = LifeOSTextHigh
                )
                Text(
                    text = "Kỹ thuật đếm giờ tập trung sâu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LifeOSTextMid
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mode Preset Tabs
        TabRow(
            selectedTabIndex = selectedPresetIndex,
            containerColor = LifeOSSurfaceCard,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedPresetIndex]),
                    color = activeColor
                )
            }
        ) {
            presets.forEachIndexed { index, (label, mins, isWork) ->
                Tab(
                    selected = selectedPresetIndex == index,
                    onClick = {
                        selectedPresetIndex = index
                        viewModel.setTime(mins, isWork)
                    },
                    text = {
                        Text(
                            text = label,
                            color = if (selectedPresetIndex == index) activeColor else LifeOSTextMid,
                            fontWeight = if (selectedPresetIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Big Circular Countdown Ring
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 14.dp.toPx()
                // Track circle
                drawCircle(
                    color = Color(0xFF131A36),
                    radius = (size.minDimension - strokeWidth) / 2,
                    style = Stroke(width = strokeWidth)
                )

                // Progress arc
                val sweep = uiState.progress * 360f
                drawArc(
                    color = activeColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.formattedTime,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Black,
                    color = LifeOSTextHigh
                )
                Text(
                    text = when {
                        !uiState.isRunning && uiState.secondsLeft == uiState.totalSeconds -> "Sẵn sàng"
                        uiState.isRunning && uiState.isWorkMode -> "Đang tập trung 🔥"
                        uiState.isRunning && !uiState.isWorkMode -> "Nghỉ ngơi ☕"
                        else -> "Đã tạm dừng ⏸"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.isRunning) activeColor else LifeOSTextMid,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Control Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset Button
            IconButton(
                onClick = { viewModel.reset() },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(LifeOSSurfaceCard)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Đặt lại",
                    tint = LifeOSTextHigh,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Play / Pause Main Button
            Button(
                onClick = { viewModel.toggleStartPause() },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                modifier = Modifier
                    .height(56.dp)
                    .width(160.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (uiState.isRunning) "Tạm dừng" else "Bắt đầu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Today's Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Số phiên hoàn thành",
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSTextMid
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.completedSessions} phiên",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LifeOSCyan
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Thời gian tập trung",
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSTextMid
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.totalMinutesFocused} phút",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = LifeOSPrimary
                    )
                }
            }
        }
    }
}
