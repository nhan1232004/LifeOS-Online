package com.nhan.lifeos.ui.vocab

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.NavigateBefore
import androidx.compose.material.icons.rounded.NavigateNext
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhan.lifeos.core.designsystem.LifeOSAmber
import com.nhan.lifeos.core.designsystem.LifeOSGlassBorder
import com.nhan.lifeos.core.designsystem.LifeOSGreen
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSRed
import com.nhan.lifeos.core.designsystem.LifeOSSurfaceCard
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import java.util.Locale

@Composable
fun VocabScreen(
    viewModel: VocabViewModel,
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    // Initialize Native Android Text-to-Speech
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var speech: TextToSpeech? = null
        speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speech?.language = Locale.US
            }
        }
        tts = speech
        onDispose {
            speech?.stop()
            speech?.shutdown()
        }
    }

    val currentCard = uiState.currentCard
    val rotation by animateFloatAsState(
        targetValue = if (uiState.isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )

    Box(modifier = Modifier.fillMaxSize()) {
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
                        text = "Từ vựng (Vocab)",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = LifeOSTextHigh
                    )
                    Text(
                        text = "Flashcard 3D & Phát âm Google TTS chuẩn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextMid
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.vocabList.isNotEmpty()) "Thẻ ${uiState.currentIndex + 1} / ${uiState.vocabList.size}" else "0 thẻ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LifeOSTextHigh
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LifeOSGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Đã thuộc: ${uiState.masteredCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LifeOSGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3D Flip Flashcard
            if (currentCard != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable { viewModel.flipCard() },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (rotation > 90f) Color(0xFF18182E) else LifeOSSurfaceCard
                    )
                ) {
                    if (rotation <= 90f) {
                        // FRONT FACE (English Word + Pronunciation + TTS)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LifeOSPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = currentCard.type.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LifeOSPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = currentCard.word,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = LifeOSTextHigh,
                                fontSize = 34.sp,
                                textAlign = TextAlign.Center
                            )

                            if (currentCard.pron.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentCard.pron,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = LifeOSTextMid,
                                    fontStyle = FontStyle.Italic
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // TTS Speaker Button
                            IconButton(
                                onClick = {
                                    tts?.speak(
                                        currentCard.word,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        currentCard.id
                                    )
                                },
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(LifeOSPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "Phát âm chuẩn",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Chạm vào thẻ để lật xem nghĩa",
                                style = MaterialTheme.typography.labelSmall,
                                color = LifeOSTextLow
                            )
                        }
                    } else {
                        // BACK FACE (Vietnamese Meaning + Example)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f } // Counter-rotate so text isn't mirrored
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Ý nghĩa tiếng Việt",
                                style = MaterialTheme.typography.labelSmall,
                                color = LifeOSGreen,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = currentCard.mean,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = LifeOSTextHigh,
                                textAlign = TextAlign.Center
                            )

                            if (currentCard.example.isNotBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF222238))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "\"${currentCard.example}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LifeOSTextMid,
                                        fontStyle = FontStyle.Italic,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action Controls: Navigation & SRS Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev Button
                    IconButton(
                        onClick = { viewModel.prevCard() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(LifeOSSurfaceCard)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NavigateBefore,
                            contentDescription = "Từ trước",
                            tint = LifeOSTextHigh,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // "Chưa nhớ" (Reset SRS)
                    Button(
                        onClick = { viewModel.markRemembered(false) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LifeOSRed.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = null, tint = LifeOSRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chưa nhớ", color = LifeOSRed, fontWeight = FontWeight.Bold)
                    }

                    // "Đã nhớ" (Advance SRS)
                    Button(
                        onClick = { viewModel.markRemembered(true) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LifeOSGreen.copy(alpha = 0.2f)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.Check, contentDescription = null, tint = LifeOSGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Đã thuộc", color = LifeOSGreen, fontWeight = FontWeight.Bold)
                    }

                    // Next Button
                    IconButton(
                        onClick = { viewModel.nextCard() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(LifeOSSurfaceCard)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NavigateNext,
                            contentDescription = "Từ kế tiếp",
                            tint = LifeOSTextHigh,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có từ vựng nào. Hãy thêm từ mới!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LifeOSTextLow
                    )
                }
            }
        }

        // FAB Add Word
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = Color(0xFFFF80AB),
            contentColor = Color.Black,
            shape = CircleShape
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = "Thêm từ vựng")
        }

        // Add Vocab Dialog
        if (showAddDialog) {
            AddVocabDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { word, pron, type, mean, ex ->
                    viewModel.addVocab(word, pron, type, mean, ex)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddVocabDialog(
    onDismiss: () -> Unit,
    onAdd: (word: String, pron: String, type: String, mean: String, example: String) -> Unit
) {
    var wordText by remember { mutableStateOf("") }
    var pronText by remember { mutableStateOf("") }
    var typeText by remember { mutableStateOf("n") }
    var meanText by remember { mutableStateOf("") }
    var exampleText by remember { mutableStateOf("") }

    val types = listOf("n", "v", "adj", "adv", "phrase")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Thêm từ vựng Flashcard mới",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LifeOSTextHigh
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = wordText,
                    onValueChange = { wordText = it },
                    label = { Text("Từ tiếng Anh (VD: Perseverance)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF80AB),
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pronText,
                        onValueChange = { pronText = it },
                        label = { Text("Phiên âm (/.../)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF80AB),
                            unfocusedBorderColor = LifeOSGlassBorder
                        )
                    )

                    OutlinedTextField(
                        value = typeText,
                        onValueChange = { typeText = it },
                        label = { Text("Từ loại") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF80AB),
                            unfocusedBorderColor = LifeOSGlassBorder
                        )
                    )
                }

                OutlinedTextField(
                    value = meanText,
                    onValueChange = { meanText = it },
                    label = { Text("Nghĩa tiếng Việt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF80AB),
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )

                OutlinedTextField(
                    value = exampleText,
                    onValueChange = { exampleText = it },
                    label = { Text("Câu ví dụ minh họa") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF80AB),
                        unfocusedBorderColor = LifeOSGlassBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (wordText.isNotBlank() && meanText.isNotBlank()) {
                        onAdd(wordText, pronText, typeText, meanText, exampleText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF80AB))
            ) {
                Text("Lưu Flashcard", color = Color.Black, fontWeight = FontWeight.Bold)
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
