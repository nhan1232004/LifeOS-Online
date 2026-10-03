package com.nhan.lifeos.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhan.lifeos.core.designsystem.LifeOSAmber
import com.nhan.lifeos.core.designsystem.LifeOSCyan
import com.nhan.lifeos.core.designsystem.LifeOSGlassBorder
import com.nhan.lifeos.core.designsystem.LifeOSGreen
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceCard
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceDark
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import com.nhan.lifeos.data.preferences.UserPreferencesRepository
import com.nhan.lifeos.data.preferences.UserSession
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userSession: UserSession,
    preferencesRepo: UserPreferencesRepository,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var displayName by remember(userSession) { mutableStateOf(userSession.displayName) }
    var jobTitle by remember(userSession) { mutableStateOf(userSession.jobTitle) }
    var phoneNumber by remember(userSession) { mutableStateOf(userSession.phoneNumber) }
    var birthday by remember(userSession) { mutableStateOf(userSession.birthday) }
    var workspace by remember(userSession) { mutableStateOf(userSession.activeWorkspace) }
    var bio by remember(userSession) { mutableStateOf(userSession.bio) }

    var isSaving by remember { mutableStateOf(false) }

    val initial = (displayName.ifBlank { userSession.userEmail }).take(1).uppercase().ifBlank { "U" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Header
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
                    text = "Thông tin cá nhân",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = LifeOSTextHigh
                )
                Text(
                    text = "Hồ sơ người dùng & Thiết lập tài khoản",
                    style = MaterialTheme.typography.bodySmall,
                    color = LifeOSTextMid
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Avatar Card & Status Badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSGlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with Camera Badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(LifeOSPrimary, LifeOSCyan)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(LifeOSSurfaceDark)
                            .border(1.5.dp, LifeOSPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Đổi ảnh",
                            tint = LifeOSCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = displayName.ifBlank { "Chưa đặt tên" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LifeOSTextHigh
                )

                Text(
                    text = userSession.userEmail.ifBlank { "Tài khoản cục bộ" },
                    style = MaterialTheme.typography.bodySmall,
                    color = LifeOSTextMid
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Online sync indicator pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (userSession.canSyncOnline) LifeOSGreen.copy(alpha = 0.15f) else LifeOSAmber.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (userSession.canSyncOnline) Icons.Rounded.CloudDone else Icons.Rounded.Info,
                        contentDescription = null,
                        tint = if (userSession.canSyncOnline) LifeOSGreen else LifeOSAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (userSession.canSyncOnline) "Cloud Sync: Đang hoạt động" else "Chế độ Khách (Offline)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (userSession.canSyncOnline) LifeOSGreen else LifeOSAmber
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Form Fields
        Text(
            text = "CHI TIẾT HỒ SƠ",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = LifeOSTextLow,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // 1. Tên hiển thị
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Họ và Tên") },
            leadingIcon = {
                Icon(Icons.Rounded.Person, contentDescription = null, tint = LifeOSPrimary)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LifeOSPrimary,
                unfocusedBorderColor = LifeOSGlassBorder,
                focusedTextColor = LifeOSTextHigh,
                unfocusedTextColor = LifeOSTextHigh
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Chức danh / Nghề nghiệp
        OutlinedTextField(
            value = jobTitle,
            onValueChange = { jobTitle = it },
            label = { Text("Chức danh / Nghề nghiệp") },
            placeholder = { Text("Ví dụ: Kỹ sư phần mềm, Sinh viên...") },
            leadingIcon = {
                Icon(Icons.Rounded.Work, contentDescription = null, tint = LifeOSCyan)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LifeOSCyan,
                unfocusedBorderColor = LifeOSGlassBorder,
                focusedTextColor = LifeOSTextHigh,
                unfocusedTextColor = LifeOSTextHigh
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Email (Read-only reference)
        OutlinedTextField(
            value = userSession.userEmail.ifBlank { "Chưa đăng nhập" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Email tài khoản") },
            leadingIcon = {
                Icon(Icons.Rounded.Email, contentDescription = null, tint = LifeOSTextMid)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LifeOSGlassBorder,
                unfocusedBorderColor = LifeOSGlassBorder,
                focusedTextColor = LifeOSTextMid,
                unfocusedTextColor = LifeOSTextMid
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Số điện thoại
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Số điện thoại") },
            placeholder = { Text("Ví dụ: 0987 654 321") },
            leadingIcon = {
                Icon(Icons.Rounded.Phone, contentDescription = null, tint = LifeOSGreen)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LifeOSGreen,
                unfocusedBorderColor = LifeOSGlassBorder,
                focusedTextColor = LifeOSTextHigh,
                unfocusedTextColor = LifeOSTextHigh
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Ngày sinh
        OutlinedTextField(
            value = birthday,
            onValueChange = { birthday = it },
            label = { Text("Ngày sinh (YYYY-MM-DD)") },
            placeholder = { Text("2000-01-01") },
            leadingIcon = {
                Icon(Icons.Rounded.Cake, contentDescription = null, tint = LifeOSAmber)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LifeOSAmber,
                unfocusedBorderColor = LifeOSGlassBorder,
                focusedTextColor = LifeOSTextHigh,
                unfocusedTextColor = LifeOSTextHigh
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 6. Không gian làm việc
        OutlinedTextField(
            value = workspace,
            onValueChange = { workspace = it },
            label = { Text("Không gian làm việc (Workspace)") },
            leadingIcon = {
                Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = LifeOSPrimary)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LifeOSPrimary,
                unfocusedBorderColor = LifeOSGlassBorder,
                focusedTextColor = LifeOSTextHigh,
                unfocusedTextColor = LifeOSTextHigh
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 7. Tiểu sử / Bio
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Giới thiệu bản thân (Bio)") },
            placeholder = { Text("Mục tiêu sống, châm ngôn hoặc ghi chú về bản thân...") },
            leadingIcon = {
                Icon(Icons.Rounded.Badge, contentDescription = null, tint = LifeOSTextMid)
            },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LifeOSPrimary,
                unfocusedBorderColor = LifeOSGlassBorder,
                focusedTextColor = LifeOSTextHigh,
                unfocusedTextColor = LifeOSTextHigh
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                if (displayName.isBlank()) {
                    Toast.makeText(context, "Vui lòng nhập tên hiển thị!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isSaving = true
                coroutineScope.launch {
                    preferencesRepo.updateProfile(
                        displayName = displayName.trim(),
                        phoneNumber = phoneNumber.trim(),
                        bio = bio.trim(),
                        jobTitle = jobTitle.trim(),
                        birthday = birthday.trim(),
                        workspace = workspace.trim().ifBlank { "Personal" }
                    )
                    isSaving = false
                    Toast.makeText(context, "Đã lưu thông tin cá nhân thành công!", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lưu thông tin",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
