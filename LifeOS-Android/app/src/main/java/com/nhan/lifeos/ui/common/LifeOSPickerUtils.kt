package com.nhan.lifeos.ui.common

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nhan.lifeos.core.designsystem.LifeOSGlassBorder
import com.nhan.lifeos.core.designsystem.LifeOSPrimary
import com.nhan.lifeos.core.designsystem.LifeOSTextHigh
import com.nhan.lifeos.core.designsystem.LifeOSTextLow
import com.nhan.lifeos.core.designsystem.LifeOSTextMid
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun showDatePicker(
    context: Context,
    initialDate: String = "",
    onDateSelected: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    if (initialDate.isNotBlank()) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdf.parse(initialDate)
            if (d != null) cal.time = d
        } catch (_: Exception) {}
    }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onDateSelected(formatted)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun showTimePicker(
    context: Context,
    initialTime: String = "",
    onTimeSelected: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    if (initialTime.isNotBlank()) {
        try {
            val parts = initialTime.split(":")
            if (parts.size >= 2) {
                cal.set(Calendar.HOUR_OF_DAY, parts[0].trim().toInt())
                cal.set(Calendar.MINUTE, parts[1].trim().toInt())
            }
        } catch (_: Exception) {}
    }
    TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val formatted = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            onTimeSelected(formatted)
        },
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE),
        true
    ).show()
}

@Composable
fun LifeOSDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Ngày thực hiện",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePicker(context, value, onValueChange) }) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = "Chọn ngày",
                    tint = LifeOSPrimary
                )
            }
        },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LifeOSPrimary,
            unfocusedBorderColor = LifeOSGlassBorder,
            focusedTextColor = LifeOSTextHigh,
            unfocusedTextColor = LifeOSTextHigh,
            focusedLabelColor = LifeOSPrimary,
            unfocusedLabelColor = LifeOSTextMid
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDatePicker(context, value, onValueChange) }
    )
}

@Composable
fun LifeOSTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Giờ",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showTimePicker(context, value, onValueChange) }) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = "Chọn giờ",
                    tint = LifeOSPrimary
                )
            }
        },
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LifeOSPrimary,
            unfocusedBorderColor = LifeOSGlassBorder,
            focusedTextColor = LifeOSTextHigh,
            unfocusedTextColor = LifeOSTextHigh,
            focusedLabelColor = LifeOSPrimary,
            unfocusedLabelColor = LifeOSTextMid
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { showTimePicker(context, value, onValueChange) }
    )
}
