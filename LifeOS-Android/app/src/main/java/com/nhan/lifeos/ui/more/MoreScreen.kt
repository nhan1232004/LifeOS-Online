package com.nhan.lifeos.ui.more

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

data class MoreModuleItem(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun MoreScreen() {
    val modules = listOf(
        MoreModuleItem("Dự án Kanban", "Quản lý tiến độ theo cột", Icons.Rounded.FolderSpecial, LifeOSPrimary),
        MoreModuleItem("Ghi chú", "Soạn thảo Markdown", Icons.Rounded.Description, LifeOSAmber),
        MoreModuleItem("Thói quen", "Ma trận & Streak", Icons.Rounded.Loop, LifeOSGreen),
        MoreModuleItem("Mục tiêu", "Theo dõi kế hoạch dài hạn", Icons.Rounded.TrackChanges, LifeOSCyan),
        MoreModuleItem("Pomodoro", "Foreground Service đếm giờ", Icons.Rounded.Speed, LifeOSRed),
        MoreModuleItem("Nhật ký", "Ghi lại suy nghĩ & Mood", Icons.Rounded.Book, Color(0xFFCE93D8)),
        MoreModuleItem("Từ vựng", "Flashcard 3D & TTS", Icons.Rounded.School, Color(0xFFFF80AB)),
        MoreModuleItem("Thống kê", "Báo cáo hiệu suất", Icons.Rounded.BarChart, Color(0xFF80D8FF)),
        MoreModuleItem("Trợ lý AI", "Gemini 1.5 Flash Chat", Icons.Rounded.AutoAwesome, LifeOSCyan),
        MoreModuleItem("Cài đặt", "Sao lưu JSON & Dữ liệu", Icons.Rounded.Settings, LifeOSTextMid)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Tất cả tính năng",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = LifeOSTextHigh
        )
        Text(
            text = "Toàn bộ hệ sinh thái LifeOS trên điện thoại của bạn",
            style = MaterialTheme.typography.bodyMedium,
            color = LifeOSTextMid,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(modules) { module ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { /* Navigate to module */ },
                    colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(module.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = module.icon,
                                contentDescription = null,
                                tint = module.color,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = module.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LifeOSTextHigh
                        )

                        Text(
                            text = module.desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = LifeOSTextLow,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
