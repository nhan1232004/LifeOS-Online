package com.nhan.lifeos.ui.today

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhan.lifeos.core.designsystem.LifeOSCyan
import com.nhan.lifeos.core.designsystem.LifeOSGreen
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceCard
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(onNavigateToFeature: (String) -> Unit = {}) {
    val dateFormat = SimpleDateFormat("EEEE, 'ngày' dd 'tháng' MM", Locale("vi", "VN"))
    val currentDateStr = dateFormat.format(Date()).replaceFirstChar { it.uppercase() }

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
                            text = currentDateStr,
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

            // Productivity KPI Card
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
                                text = "80%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LifeOSGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem(label = "Việc hoàn thành", value = "4/5")
                            StatItem(label = "Thời gian tập trung", value = "1h 45m")
                            StatItem(label = "Streak thói quen", value = "12 ngày")
                        }
                    }
                }
            }

            // Quick Agenda Section Title
            item {
                Text(
                    text = "Lịch trình sắp tới",
                    style = MaterialTheme.typography.titleLarge,
                    color = LifeOSTextHigh,
                    fontWeight = FontWeight.Bold
                )
            }

            // Sample Native Event Card
            item {
                AgendaItemCard(
                    time = "09:00 - 10:30",
                    title = "Họp chiến lược sản phẩm Q3",
                    type = "Công việc",
                    typeColor = LifeOSPrimary
                )
            }

            item {
                AgendaItemCard(
                    time = "14:00 - 15:00",
                    title = "Review Code Architecture Jetpack Compose",
                    type = "Học tập",
                    typeColor = LifeOSCyan
                )
            }

            item {
                AgendaItemCard(
                    time = "17:30 - 19:00",
                    title = "Tập Gym & Cardio",
                    type = "Sức khoẻ",
                    typeColor = LifeOSGreen
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Quick Add FAB
        FloatingActionButton(
            onClick = { /* Quick Add */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = LifeOSPrimary,
            contentColor = Color.White
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm nhanh")
        }
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
