package com.nhan.lifeos.ui.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextMid

@Composable
fun CalendarScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Lịch & Sự kiện",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = LifeOSTextHigh
        )
        Text(
            text = "Quản lý tiến độ và thời gian biểu",
            style = MaterialTheme.typography.bodyMedium,
            color = LifeOSTextMid,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📅 Giao diện Lịch Native đang được kết nối Room Database",
                style = MaterialTheme.typography.bodyLarge,
                color = LifeOSTextMid
            )
        }
    }
}
