package com.arka.vpn.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arka.vpn.model.ConnectionMode
import com.arka.vpn.ui.theme.AccentRed

@Composable
fun ManageConfigsDialog(
    counts: Map<ConnectionMode, Int>,
    onImportAssets: () -> Unit,
    onImportClipboard: () -> Unit,
    onClearDatabase: () -> Unit,
    onDismiss: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مدیریت کانفیگ‌ها") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ConnectionMode.entries.forEach { mode ->
                    Text("${mode.label} —  ${counts[mode] ?: 0} کانفیگ")
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onImportAssets) { Text("📥 ایمپورت از فایل‌ها") }
                TextButton(onClick = onImportClipboard) { Text("📋 ایمپورت از کلیپ‌بورد") }
                TextButton(onClick = { showClearConfirm = true }) {
                    Text("🗑 پاک کردن دیتابیس", color = AccentRed)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("پاک کردن دیتابیس") },
            text = { Text("همه کانفیگ‌های ذخیره‌شده در همه بخش‌ها حذف شوند؟ این کار قابل بازگشت نیست.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    onClearDatabase()
                }) { Text("بله، پاک کن", color = AccentRed) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("انصراف") } }
        )
    }
}

@Composable
fun ClipboardCategoryDialog(
    onPick: (ConnectionMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("این کانفیگ‌ها به کدام بخش اضافه شوند؟") },
        text = {
            Column {
                ConnectionMode.entries.forEach { mode ->
                    TextButton(onClick = { onPick(mode) }, modifier = Modifier.fillMaxWidth()) {
                        Text(mode.label, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun PrivateActivationDialog(
    onActivate: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("فعال‌سازی سرور شخصی") },
        text = {
            Column {
                Text("لینک یا کد اختصاصی خود را وارد کنید. این اطلاعات نمایش داده نمی‌شود و فقط وضعیت شما را فعال می‌کند.")
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    singleLine = true,
                    placeholder = { Text("Private link / code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(error ?: "", color = Color(0xFFFF6B7A))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!onActivate(text)) {
                    error = "لینک یا کد را کامل وارد کنید (حداقل ۶ کاراکتر)"
                } else {
                    onDismiss()
                }
            }) { Text("فعال‌سازی") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
