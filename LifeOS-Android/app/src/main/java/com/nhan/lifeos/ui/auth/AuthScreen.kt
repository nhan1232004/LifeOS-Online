package com.nhan.lifeos.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhan.lifeos.core.designsystem.LifeOSCyan
import com.nhan.lifeos.core.designsystem.LifeOSGlassBorder
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceCard
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceDark
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import com.nhan.lifeos.data.network.FirebaseAuthService
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onSignInSuccess: (email: String, name: String, uid: String, idToken: String, refreshToken: String) -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val authService = remember { FirebaseAuthService() }

    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showGoogleAdviceDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(LifeOSPrimary, LifeOSCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FlashOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "LifeOS",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = LifeOSTextHigh
            )

            Text(
                text = "Không gian quản lý cuộc sống & công việc toàn diện",
                style = MaterialTheme.typography.bodyMedium,
                color = LifeOSTextMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Platform badge
            Row(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(LifeOSCyan.copy(alpha = 0.12f))
                    .border(1.dp, LifeOSCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Smartphone,
                    contentDescription = null,
                    tint = LifeOSCyan,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bản Ứng Dụng Di Động Native",
                    style = MaterialTheme.typography.labelSmall,
                    color = LifeOSCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceCard)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Segmented Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(4.dp)
                    ) {
                        TabButton(
                            text = "Đăng nhập",
                            selected = !isRegisterMode,
                            onClick = {
                                isRegisterMode = false
                                errorMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        TabButton(
                            text = "Đăng ký mới",
                            selected = isRegisterMode,
                            onClick = {
                                isRegisterMode = true
                                errorMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Họ và tên") },
                            leadingIcon = {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = LifeOSTextMid)
                            },
                            singleLine = true,
                            colors = customTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Email, contentDescription = null, tint = LifeOSTextMid)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = customTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mật khẩu") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = LifeOSTextMid)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    contentDescription = null,
                                    tint = LifeOSTextMid
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = customTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    errorMessage?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Vui lòng nhập đầy đủ Email và Mật khẩu"
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = "Mật khẩu cần tối thiểu 6 ký tự"
                                return@Button
                            }
                            val name = if (isRegisterMode && fullName.isNotBlank()) fullName else email.substringBefore("@")
                            
                            isLoading = true
                            errorMessage = null
                            coroutineScope.launch {
                                val result = if (isRegisterMode) {
                                    authService.signUp(email.trim(), password, name)
                                } else {
                                    authService.signIn(email.trim(), password)
                                }
                                isLoading = false
                                if (result.success) {
                                    onSignInSuccess(
                                        result.email,
                                        result.displayName,
                                        result.userId,
                                        result.idToken,
                                        result.refreshToken
                                    )
                                } else {
                                    errorMessage = result.errorMessage ?: "Đã có lỗi xảy ra. Vui lòng thử lại."
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LifeOSPrimary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "Tạo tài khoản mới" else "Đăng nhập vào LifeOS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    // Divider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = LifeOSGlassBorder)
                        Text(
                            text = "HOẶC",
                            style = MaterialTheme.typography.labelSmall,
                            color = LifeOSTextLow,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = LifeOSGlassBorder)
                    }

                    // Guest Instant Entry Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onContinueAsGuest() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSPrimary.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LifeOSPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FlashOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dùng ngay không cần đăng nhập",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = LifeOSTextHigh
                                )
                                Text(
                                    text = "Trải nghiệm tức thì • Lưu offline trên máy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LifeOSTextMid
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = LifeOSTextLow
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Google Login button (with advice dialog on mobile)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showGoogleAdviceDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LifeOSSurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LifeOSGlassBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Đăng nhập với Google",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LifeOSTextHigh
                            )
                        }
                    }
                }
            }
        }
    }

    // Google Advice Dialog
    if (showGoogleAdviceDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleAdviceDialog = false },
            title = {
                Text(
                    text = "Trải nghiệm Ứng dụng Di động",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Để ứng dụng hoạt động độc lập và mượt mà nhất (không bị phụ thuộc vào cửa sổ trình duyệt bên ngoài):\n\n1. Đăng nhập bằng Email & Mật khẩu: Dữ liệu của bạn từ phiên bản Web sẽ được đồng bộ 2 chiều tự động qua internet về máy.\n\n2. Chọn 'Dùng ngay không cần đăng nhập' để sử dụng toàn bộ tính năng với dữ liệu lưu an toàn trên máy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LifeOSTextMid
                )
            },
            confirmButton = {
                TextButton(onClick = { showGoogleAdviceDialog = false }) {
                    Text("Đã hiểu", color = LifeOSPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = LifeOSSurfaceCard
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) LifeOSSurfaceDark else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) LifeOSTextHigh else LifeOSTextLow
        )
    }
}

@Composable
private fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LifeOSPrimary,
    unfocusedBorderColor = LifeOSGlassBorder,
    focusedLabelColor = LifeOSPrimary,
    unfocusedLabelColor = LifeOSTextLow,
    cursorColor = LifeOSPrimary,
    focusedTextColor = LifeOSTextHigh,
    unfocusedTextColor = LifeOSTextHigh
)
