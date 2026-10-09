package com.nhan.lifeos.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.nhan.lifeos.core.designsystem.LifeOSGlassBorder
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSRed
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceCard
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import com.nhan.lifeos.data.local.entity.NoteEntity

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }
    var selectedCategory by remember { mutableStateOf("all") }

    val categories = listOf(
        "all" to "Tất cả",
        "work" to "Công việc",
        "personal" to "Cá nhân",
        "excel" to "Bảng tính Excel"
    )

    val displayedNotes = remember(uiState.notes, selectedCategory) {
        when (selectedCategory) {
            "work" -> uiState.notes.filter { note ->
                note.tags.any { it.contains("work", true) || it.contains("công việc", true) } ||
                note.title.contains("công việc", true)
            }
            "personal" -> uiState.notes.filter { note ->
                note.tags.any { it.contains("personal", true) || it.contains("cá nhân", true) } ||
                note.title.contains("cá nhân", true)
            }
            "excel" -> uiState.notes.filter { note ->
                note.tags.any { it.contains("excel", true) || it.contains("sheet", true) || it.contains("bảng", true) } ||
                note.title.contains("excel", true) || note.content.contains("sheet", true)
            }
            else -> uiState.notes
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                            text = "Ghi chú (Notes)",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = LifeOSTextHigh
                        )
                        Text(
                            text = "Chạm vào ghi chú để xem chi tiết & chỉnh sửa",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LifeOSTextMid
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Tìm kiếm tiêu đề, nội dung, thẻ...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = LifeOSTextMid
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Xóa tìm kiếm",
                                    tint = LifeOSTextMid
                                )
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = LifeOSSurfaceCard,
                        unfocusedContainerColor = LifeOSSurfaceCard,
                        focusedBorderColor = LifeOSPrimary,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )
            }

            // Category Filter Pills
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { (key, label) ->
                        val isSelected = selectedCategory == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) LifeOSPrimary else LifeOSSurfaceCard)
                                .clickable { selectedCategory = key }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else LifeOSTextMid
                            )
                        }
                    }
                }
            }

            // Notes List
            if (displayedNotes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "Không tìm thấy ghi chú phù hợp" else "Chưa có ghi chú nào trong mục này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LifeOSTextLow
                        )
                    }
                }
            } else {
                items(displayedNotes, key = { it.id }) { note ->
                    NoteCardItem(
                        note = note,
                        onClick = { editingNote = note },
                        onTogglePin = { viewModel.togglePin(note.id) },
                        onDelete = { viewModel.deleteNote(note.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // FAB Add Note
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = LifeOSPrimary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm ghi chú")
        }

        // Add Note Dialog
        if (showAddDialog) {
            AddNoteDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, content, tags, pinned ->
                    viewModel.addNote(title, content, tags, pinned)
                    showAddDialog = false
                }
            )
        }

        // Detail & Edit Note Dialog
        editingNote?.let { noteToEdit ->
            EditNoteDialog(
                note = noteToEdit,
                onDismiss = { editingNote = null },
                onSave = { updated ->
                    viewModel.updateNote(updated)
                    editingNote = null
                },
                onDelete = {
                    viewModel.deleteNote(noteToEdit.id)
                    editingNote = null
                }
            )
        }
    }
}

@Composable
fun NoteCardItem(
    note: NoteEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (note.pinned) Color(0xFF1B1B2F) else LifeOSSurfaceCard
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Không có tiêu đề" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LifeOSTextHigh,
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = if (note.pinned) "Bỏ ghim" else "Ghim",
                            tint = if (note.pinned) LifeOSAmber else LifeOSTextLow,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Xóa",
                            tint = LifeOSTextLow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = LifeOSTextMid,
                maxLines = 4
            )

            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(note.tags) { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(LifeOSPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = LifeOSPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditNoteDialog(
    note: NoteEntity,
    onDismiss: () -> Unit,
    onSave: (NoteEntity) -> Unit,
    onDelete: () -> Unit
) {
    var titleText by remember { mutableStateOf(note.title) }
    var contentText by remember { mutableStateOf(note.content) }
    var tagsText by remember { mutableStateOf(note.tags.joinToString(", ")) }
    var pinned by remember { mutableStateOf(note.pinned) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chi tiết & Chỉnh sửa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LifeOSTextHigh
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Xóa ghi chú",
                        tint = LifeOSRed
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Tiêu đề ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSAmber,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text("Nội dung ghi chú") },
                    minLines = 6,
                    maxLines = 14,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSAmber,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Thẻ phân loại (cách nhau bởi dấu phẩy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSAmber,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = pinned,
                        onCheckedChange = { pinned = it },
                        colors = CheckboxDefaults.colors(checkedColor = LifeOSAmber)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ghim lên đầu trang",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextHigh
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tagsList = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(
                        note.copy(
                            title = titleText.trim(),
                            content = contentText.trim(),
                            tags = tagsList,
                            pinned = pinned,
                            updatedAt = System.currentTimeMillis(),
                            isSynced = false
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSAmber)
            ) {
                Text("Lưu thay đổi", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = LifeOSTextMid)
            }
        },
        containerColor = Color(0xFF141424),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, content: String, tags: List<String>, pinned: Boolean) -> Unit
) {
    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tạo ghi chú mới",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextHigh
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Tiêu đề ghi chú") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSAmber,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text("Nội dung ghi chú") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSAmber,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Thẻ phân loại (cách nhau bởi dấu phẩy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LifeOSAmber,
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = pinned,
                        onCheckedChange = { pinned = it },
                        colors = CheckboxDefaults.colors(checkedColor = LifeOSAmber)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ghim lên đầu trang",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextHigh
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleText.isNotBlank() || contentText.isNotBlank()) {
                        val tagsList = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onAdd(titleText, contentText, tagsList, pinned)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = LifeOSAmber)
            ) {
                Text("Lưu ghi chú", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = LifeOSTextMid)
            }
        },
        containerColor = Color(0xFF141424),
        shape = RoundedCornerShape(20.dp)
    )
}
