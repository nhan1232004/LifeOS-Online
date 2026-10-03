package com.nhan.lifeos.ui.projects

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderSpecial
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
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.nhan.lifeos.data.local.entity.ProjectEntity

@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val tabs = listOf(
        "Cần làm (${uiState.todoProjects.size})",
        "Đang làm (${uiState.inProgressProjects.size})",
        "Hoàn thành (${uiState.doneProjects.size})"
    )

    val currentList = when (selectedTabIndex) {
        0 -> uiState.todoProjects
        1 -> uiState.inProgressProjects
        2 -> uiState.doneProjects
        else -> emptyList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = LifeOSTextHigh
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Dự án Kanban",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = LifeOSTextHigh
                    )
                    Text(
                        text = "Tiến độ công việc theo các cột trực quan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextMid
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Column Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = LifeOSPrimary,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = LifeOSPrimary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) LifeOSPrimary else LifeOSTextMid
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Project List for active column
            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có dự án nào trong cột này",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LifeOSTextLow
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(currentList, key = { it.id }) { project ->
                        ProjectCardItem(
                            project = project,
                            onMove = { viewModel.moveProject(project.id, project.status) },
                            onDelete = { viewModel.deleteProject(project.id) }
                        )
                    }
                }
            }
        }

        // Add Project FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = LifeOSPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Thêm dự án")
        }
    }

    if (showAddDialog) {
        AddProjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, priority, due, desc ->
                val initialStatus = when (selectedTabIndex) {
                    1 -> "Đang làm"
                    2 -> "Hoàn thành"
                    else -> "Cần làm"
                }
                viewModel.addProject(
                    name = name,
                    status = initialStatus,
                    priority = priority,
                    due = due,
                    desc = desc
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ProjectCardItem(
    project: ProjectEntity,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val priColor = when (project.priority.lowercase()) {
        "high" -> LifeOSRed
        "mid" -> LifeOSAmber
        else -> LifeOSGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LifeOSTextHigh,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(priColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = project.priority.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = priColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (project.desc.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = project.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LifeOSTextMid,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (project.due.isNotBlank()) {
                    Text(
                        text = "Hạn: ${project.due}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LifeOSTextLow
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Xóa",
                            tint = LifeOSTextLow,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = onMove,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Chuyển cột", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, priority: String, due: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("high") }
    var due by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Thêm Dự án mới", fontWeight = FontWeight.Bold, color = LifeOSTextHigh)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên dự án *") },
                    singleLine = true,
                    colors = customDialogTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Priority row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("high" to "Cao", "mid" to "TB", "low" to "Thấp").forEach { (pri, label) ->
                        val isSel = priority == pri
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LifeOSPrimary else Color.White.copy(alpha = 0.05f))
                                .clickable { priority = pri }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else LifeOSTextMid
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = due,
                    onValueChange = { due = it },
                    label = { Text("Hạn chót (YYYY-MM-DD)") },
                    singleLine = true,
                    colors = customDialogTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Mô tả chi tiết") },
                    colors = customDialogTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim(), priority, due.trim(), desc.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
            ) {
                Text("Lưu dự án", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LifeOSTextMid)
            }
        },
        containerColor = LifeOSSurfaceCard
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
