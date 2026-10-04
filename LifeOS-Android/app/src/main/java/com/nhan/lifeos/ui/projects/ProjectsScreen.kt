package com.nhan.lifeos.ui.projects

import androidx.compose.foundation.background
import com.nhan.lifeos.ui.common.LifeOSDateField
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.ui.platform.LocalContext
import com.nhan.lifeos.data.preferences.UserPreferencesRepository
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceDark
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import com.nhan.lifeos.data.local.entity.ProjectEntity
import com.nhan.lifeos.data.local.entity.ProjectTaskEntity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showEditProjectDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showInviteMemberDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<ProjectTaskEntity?>(null) }

    val context = LocalContext.current
    val prefsRepo = remember { UserPreferencesRepository(context) }
    val sessionState by prefsRepo.userSessionFlow.collectAsState(initial = null)
    val currentSenderEmail = sessionState?.userEmail ?: "local@lifeos.app"
    val currentSenderName = sessionState?.displayName?.ifBlank { "Người dùng" } ?: "Người dùng"

    val activeProject = uiState.selectedProject

    val filterTabs = listOf(
        "Tất cả" to "all",
        "Cần làm" to "Cần làm",
        "Đang làm" to "Đang làm",
        "Hoàn thành" to "Hoàn thành"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Bar
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quản lý Dự án & Kanban",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = LifeOSTextHigh
                    )
                    Text(
                        text = "Tiến độ công việc & nhiệm vụ chi tiết",
                        style = MaterialTheme.typography.bodySmall,
                        color = LifeOSTextMid
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Projects Horizontal Selector
            Text(
                text = "DANH SÁCH DỰ ÁN",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextLow,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.projects, key = { it.id }) { proj ->
                    val isSelected = proj.id == activeProject?.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LifeOSPrimary else LifeOSSurfaceDark)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) LifeOSPrimary else LifeOSGlassBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectProject(proj.id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else LifeOSCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = proj.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else LifeOSTextHigh
                            )
                        }
                    }
                }

                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(LifeOSSurfaceDark)
                            .border(1.dp, LifeOSPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { showAddProjectDialog = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = LifeOSPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Thêm dự án",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = LifeOSPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Active Project Overview Card
            if (activeProject != null) {
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
                            Text(
                                text = activeProject.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = LifeOSTextHigh,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(
                                    onClick = { showEditProjectDialog = true },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Sửa",
                                        tint = LifeOSTextMid,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteProject(activeProject.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Xóa",
                                        tint = LifeOSRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (activeProject.desc.isNotBlank()) {
                            Text(
                                text = activeProject.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = LifeOSTextMid,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                            )
                        }

                        // Progress Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tiến độ: ${uiState.projectProgress}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = LifeOSPrimary
                            )
                            Text(
                                text = "${uiState.completedTasksCount}/${uiState.totalTasksCount} nhiệm vụ hoàn thành",
                                style = MaterialTheme.typography.labelSmall,
                                color = LifeOSTextLow
                            )
                        }
                        LinearProgressIndicator(
                            progress = { uiState.projectProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = LifeOSPrimary,
                            trackColor = LifeOSGlassBorder
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Meta Badges (Due, Budget, Priority)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeProject.due.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarToday,
                                        contentDescription = null,
                                        tint = LifeOSTextLow,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeProject.due,
                                        fontSize = 11.5.sp,
                                        color = LifeOSTextMid
                                    )
                                }
                            }

                            if (activeProject.budget > 0) {
                                val fmt = DecimalFormat("#,###")
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.MonetizationOn,
                                        contentDescription = null,
                                        tint = LifeOSAmber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${fmt.format(activeProject.budget)}₫",
                                        fontSize = 11.5.sp,
                                        color = LifeOSAmber
                                    )
                                }
                            }

                            val priColor = when (activeProject.priority.lowercase()) {
                                "high" -> LifeOSRed
                                "mid" -> LifeOSAmber
                                else -> LifeOSGreen
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(priColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Ưu tiên: ${activeProject.priority}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = priColor
                                )
                            }
                        }

                        // ─── Team Members & Collaboration ───
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = LifeOSGlassBorder)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Group,
                                    contentDescription = null,
                                    tint = LifeOSCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Thành viên (${activeProject.members.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LifeOSTextMid
                                )
                            }
                        }

                        if (activeProject.members.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(activeProject.members) { email ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(LifeOSSurfaceDark)
                                            .border(1.dp, LifeOSGlassBorder, RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(LifeOSPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = email.take(1).uppercase(),
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = email,
                                            fontSize = 11.sp,
                                            color = LifeOSTextHigh
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Xóa",
                                            tint = LifeOSTextLow,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { viewModel.removeMember(activeProject, email) }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Buttons: Mời thành viên & Chat nhóm
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showInviteMemberDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LifeOSCyan.copy(alpha = 0.15f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PersonAdd,
                                    contentDescription = null,
                                    tint = LifeOSCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mời thành viên",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LifeOSCyan
                                )
                            }

                            Button(
                                onClick = { showChatDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary.copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Forum,
                                    contentDescription = null,
                                    tint = LifeOSPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Chat nhóm",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LifeOSPrimary
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có dự án nào. Bấm 'Thêm dự án' để bắt đầu!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextLow
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Task Status Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = filterTabs.indexOfFirst { it.second == uiState.taskFilter }.coerceAtLeast(0),
                containerColor = Color.Transparent,
                contentColor = LifeOSPrimary,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    val idx = filterTabs.indexOfFirst { it.second == uiState.taskFilter }.coerceAtLeast(0)
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[idx]),
                        color = LifeOSPrimary
                    )
                }
            ) {
                filterTabs.forEach { (label, value) ->
                    val count = when (value) {
                        "all" -> uiState.allTasks.size
                        "Cần làm" -> uiState.allTasks.count { it.status == "Cần làm" || it.status == "Khởi tạo" || it.status == "Backlog" }
                        "Đang làm" -> uiState.allTasks.count { it.status == "Đang làm" || it.status == "In Progress" }
                        "Hoàn thành" -> uiState.allTasks.count { it.status == "Hoàn thành" || it.status == "Done" }
                        else -> 0
                    }
                    val isSelected = uiState.taskFilter == value
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setTaskFilter(value) },
                        text = {
                            Text(
                                text = "$label ($count)",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) LifeOSPrimary else LifeOSTextMid,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Tasks List
            if (uiState.displayTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Không có nhiệm vụ nào trong mục này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LifeOSTextLow
                        )
                        if (activeProject != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { showAddTaskDialog = true }) {
                                Text("+ Thêm nhiệm vụ vào dự án", color = LifeOSPrimary)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.displayTasks, key = { it.id }) { task ->
                        ProjectTaskCard(
                            task = task,
                            onCycleStatus = { viewModel.cycleTaskStatus(task) },
                            onEdit = { editingTask = task },
                            onDelete = { viewModel.deleteTask(task.id) }
                        )
                    }
                }
            }
        }

        // Add Task FAB
        if (activeProject != null) {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                containerColor = LifeOSPrimary,
                contentColor = Color.White
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, contentDescription = "Thêm nhiệm vụ")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nhiệm vụ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    // Add Project Dialog
    if (showAddProjectDialog) {
        ProjectFormDialog(
            title = "Tạo dự án mới",
            initialName = "",
            initialDesc = "",
            initialPriority = "mid",
            initialDue = "",
            initialBudget = 0L,
            onDismiss = { showAddProjectDialog = false },
            onConfirm = { name, desc, pri, due, budget ->
                viewModel.addProject(
                    name = name,
                    status = "Cần làm",
                    priority = pri,
                    budget = budget,
                    due = due,
                    desc = desc
                )
                showAddProjectDialog = false
            }
        )
    }

    // Edit Project Dialog
    if (showEditProjectDialog && activeProject != null) {
        ProjectFormDialog(
            title = "Chỉnh sửa dự án",
            initialName = activeProject.name,
            initialDesc = activeProject.desc,
            initialPriority = activeProject.priority,
            initialDue = activeProject.due,
            initialBudget = activeProject.budget,
            onDismiss = { showEditProjectDialog = false },
            onConfirm = { name, desc, pri, due, budget ->
                viewModel.updateProject(
                    activeProject.copy(
                        name = name,
                        desc = desc,
                        priority = pri,
                        due = due,
                        budget = budget
                    )
                )
                showEditProjectDialog = false
            }
        )
    }

    // Add Task Dialog
    if (showAddTaskDialog && activeProject != null) {
        AddTaskDialog(
            projectName = activeProject.name,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { text, priority, due, desc ->
                val initialStatus = when (uiState.taskFilter) {
                    "Đang làm" -> "Đang làm"
                    "Hoàn thành" -> "Hoàn thành"
                    else -> "Cần làm"
                }
                viewModel.addProjectTask(
                    projId = activeProject.id,
                    text = text,
                    status = initialStatus,
                    priority = priority,
                    desc = desc,
                    due = due
                )
                showAddTaskDialog = false
            }
        )
    }

    // Invite Member Dialog
    if (showInviteMemberDialog && activeProject != null) {
        var emailInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showInviteMemberDialog = false },
            title = {
                Text(
                    text = "Mời thành viên vào dự án",
                    fontWeight = FontWeight.Bold,
                    color = LifeOSTextHigh
                )
            },
            text = {
                Column {
                    Text(
                        text = "Dự án: ${activeProject.name}",
                        fontSize = 13.sp,
                        color = LifeOSCyan,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Địa chỉ Email") },
                        placeholder = { Text("vd: teammate@gmail.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LifeOSPrimary,
                            unfocusedBorderColor = LifeOSGlassBorder,
                            focusedTextColor = LifeOSTextHigh,
                            unfocusedTextColor = LifeOSTextHigh
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            viewModel.addMember(activeProject, emailInput.trim().lowercase())
                            showInviteMemberDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
                ) {
                    Text("Thêm vào dự án", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteMemberDialog = false }) {
                    Text("Hủy", color = LifeOSTextMid)
                }
            },
            containerColor = LifeOSSurfaceDark
        )
    }

    // Project Chat Dialog
    if (showChatDialog && activeProject != null) {
        ProjectChatDialog(
            project = activeProject,
            viewModel = viewModel,
            currentUserEmail = currentSenderEmail,
            currentUserName = currentSenderName,
            onDismiss = { showChatDialog = false }
        )
    }

    // Edit Project Task Dialog
    if (editingTask != null) {
        val currentEditTask = editingTask!!
        EditProjectTaskDialog(
            task = currentEditTask,
            onDismiss = { editingTask = null },
            onSave = { updated ->
                viewModel.updateProjectTask(updated)
                editingTask = null
            },
            onDelete = {
                viewModel.deleteTask(currentEditTask.id)
                editingTask = null
            }
        )
    }
}

@Composable
private fun ProjectTaskCard(
    task: ProjectTaskEntity,
    onCycleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isDone = task.status == "Hoàn thành" || task.status == "Done"
    val isInProgress = task.status == "Đang làm" || task.status == "In Progress"

    val statusColor = when {
        isDone -> LifeOSGreen
        isInProgress -> LifeOSAmber
        else -> LifeOSCyan
    }

    val priColor = when (task.priority.lowercase()) {
        "high" -> LifeOSRed
        "mid" -> LifeOSAmber
        else -> LifeOSGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSGlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon Clicker
            IconButton(
                onClick = onCycleStatus,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = "Đổi trạng thái",
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDone) LifeOSTextLow else LifeOSTextHigh
                )

                if (task.desc.isNotBlank()) {
                    Text(
                        text = task.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = LifeOSTextMid,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Badge (click to cycle)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .clickable { onCycleStatus() }
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${task.status} ➔",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    // Priority Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priColor
                        )
                    }

                    if (task.due.isNotBlank()) {
                        Text(
                            text = "📅 ${task.due}",
                            fontSize = 11.sp,
                            color = LifeOSTextLow
                        )
                    }
                }
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Sửa nhiệm vụ",
                    tint = LifeOSTextLow,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Xóa nhiệm vụ",
                    tint = LifeOSTextLow,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EditProjectTaskDialog(
    task: ProjectTaskEntity,
    onDismiss: () -> Unit,
    onSave: (ProjectTaskEntity) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember { mutableStateOf(task.text) }
    var desc by remember { mutableStateOf(task.desc) }
    var priority by remember { mutableStateOf(task.priority) }
    var due by remember { mutableStateOf(task.due) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LifeOSSurfaceDark,
        title = {
            Text(
                text = "Chỉnh sửa nhiệm vụ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextHigh
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Tên nhiệm vụ *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Chi tiết bổ sung") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low" to "Thấp", "mid" to "Vừa", "high" to "Cao").forEach { (priKey, priLbl) ->
                        val isSel = priority.lowercase() == priKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LifeOSPrimary else LifeOSSurfaceDark)
                                .clickable { priority = priKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = priLbl,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else LifeOSTextMid,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                LifeOSDateField(
                    value = due,
                    onValueChange = { due = it },
                    label = "Hạn chót nhiệm vụ"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSave(
                            task.copy(
                                text = text.trim(),
                                desc = desc.trim(),
                                priority = priority,
                                due = due.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
            ) {
                Text("Lưu thay đổi", color = Color.White)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Xóa", color = LifeOSRed)
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy", color = LifeOSTextMid)
                }
            }
        }
    )
}

@Composable
private fun ProjectFormDialog(
    title: String,
    initialName: String,
    initialDesc: String,
    initialPriority: String,
    initialDue: String,
    initialBudget: Long,
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String, priority: String, due: String, budget: Long) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var desc by remember { mutableStateOf(initialDesc) }
    var priority by remember { mutableStateOf(initialPriority) }
    var due by remember { mutableStateOf(initialDue) }
    var budgetStr by remember { mutableStateOf(if (initialBudget > 0) initialBudget.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, color = LifeOSTextHigh)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên dự án *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Mô tả dự án") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low" to "Thấp", "mid" to "Vừa", "high" to "Cao").forEach { (priKey, priLbl) ->
                        val isSel = priority.lowercase() == priKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LifeOSPrimary else LifeOSSurfaceDark)
                                .clickable { priority = priKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = priLbl,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else LifeOSTextMid,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                LifeOSDateField(
                    value = due,
                    onValueChange = { due = it },
                    label = "Hạn chót dự án"
                )

                OutlinedTextField(
                    value = budgetStr,
                    onValueChange = { budgetStr = it },
                    label = { Text("Ngân sách (₫)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val budget = budgetStr.toLongOrNull() ?: 0L
                        onConfirm(name.trim(), desc.trim(), priority, due.trim(), budget)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
            ) {
                Text("Lưu dự án")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LifeOSTextMid)
            }
        }
    )
}

@Composable
private fun AddTaskDialog(
    projectName: String,
    onDismiss: () -> Unit,
    onConfirm: (text: String, priority: String, due: String, desc: String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("mid") }
    var due by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Thêm nhiệm vụ", fontWeight = FontWeight.Bold, color = LifeOSTextHigh)
                Text("Dự án: $projectName", style = MaterialTheme.typography.bodySmall, color = LifeOSPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Nội dung nhiệm vụ *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Chi tiết bổ sung") },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low" to "Thấp", "mid" to "Vừa", "high" to "Cao").forEach { (priKey, priLbl) ->
                        val isSel = priority.lowercase() == priKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) LifeOSPrimary else LifeOSSurfaceDark)
                                .clickable { priority = priKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = priLbl,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else LifeOSTextMid,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                LifeOSDateField(
                    value = due,
                    onValueChange = { due = it },
                    label = "Hạn chót nhiệm vụ"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text.trim(), priority, due.trim(), desc.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
            ) {
                Text("Thêm nhiệm vụ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LifeOSTextMid)
            }
        }
    )
}

@Composable
private fun ProjectChatDialog(
    project: ProjectEntity,
    viewModel: ProjectsViewModel,
    currentUserEmail: String,
    currentUserName: String,
    onDismiss: () -> Unit
) {
    val messages by viewModel.getProjectMessages(project.id).collectAsState(initial = emptyList())
    var inputText by remember { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    androidx.compose.runtime.LaunchedEffect(project.id) {
        viewModel.syncProjectChat(project.id)
        while (true) {
            kotlinx.coroutines.delay(3000L)
            viewModel.syncProjectChat(project.id)
        }
    }

    androidx.compose.runtime.LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(LifeOSPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Forum,
                            contentDescription = null,
                            tint = LifeOSPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Kênh chat thảo luận",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = LifeOSTextHigh
                        )
                        Text(
                            text = project.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = LifeOSCyan
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Đóng",
                        tint = LifeOSTextMid,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages List
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Forum,
                                contentDescription = null,
                                tint = LifeOSTextLow,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chưa có tin nhắn nào trong kênh chat này.",
                                fontSize = 12.5.sp,
                                color = LifeOSTextLow
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hãy gửi tin nhắn trao đổi cùng các thành viên!",
                                fontSize = 11.5.sp,
                                color = LifeOSCyan
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isMe = msg.senderEmail == currentUserEmail || (currentUserEmail.isBlank() && msg.senderEmail == "local@lifeos.app")
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Text(
                                        text = if (isMe) "Bạn" else msg.senderName.ifBlank { msg.senderEmail },
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isMe) LifeOSCyan else LifeOSTextMid
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = timeFmt.format(Date(msg.timestamp)),
                                        fontSize = 9.5.sp,
                                        color = LifeOSTextLow
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isMe) 12.dp else 2.dp,
                                                bottomEnd = if (isMe) 2.dp else 12.dp
                                            )
                                        )
                                        .background(if (isMe) LifeOSPrimary else LifeOSSurfaceDark)
                                        .border(
                                            1.dp,
                                            if (isMe) LifeOSPrimary else LifeOSGlassBorder,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 13.sp,
                                        color = if (isMe) Color.White else LifeOSTextHigh
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Input Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Nhập tin nhắn...", fontSize = 12.5.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LifeOSPrimary,
                            unfocusedBorderColor = LifeOSGlassBorder,
                            focusedTextColor = LifeOSTextHigh,
                            unfocusedTextColor = LifeOSTextHigh
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendChatMessage(
                                    projId = project.id,
                                    senderEmail = currentUserEmail,
                                    senderName = currentUserName,
                                    text = inputText.trim()
                                )
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(LifeOSPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Gửi",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = LifeOSSurfaceCard
    )
}
