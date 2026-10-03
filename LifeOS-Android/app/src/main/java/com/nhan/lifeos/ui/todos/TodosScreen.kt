package com.nhan.lifeos.ui.todos

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
fun TodosScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            text = "Việc cần làm",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = LifeOSTextHigh
        )
        Text(
            text = "Danh sách nhiệm vụ & sắp xếp mức độ ưu tiên",
            style = MaterialTheme.typography.bodyMedium,
            color = LifeOSTextMid,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡ Danh sách To-do Native với cử chỉ Vuốt để hoàn thành",
                style = MaterialTheme.typography.bodyLarge,
                color = LifeOSTextMid
            )
        }
    }
}
