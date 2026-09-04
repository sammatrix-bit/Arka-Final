package com.arka.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arka.vpn.ui.theme.AccentBlue
import com.arka.vpn.ui.theme.LineColor
import com.arka.vpn.ui.theme.PanelSurface
import com.arka.vpn.ui.theme.TextMuted
import com.arka.vpn.ui.theme.TextPrimary

@Composable
fun ArkaTopBar(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1A2138), RoundedCornerShape(13.dp))
                    .border(1.dp, LineColor, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
            }
            Column {
                Text("آرکا", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("اتصال امن و پایدار", color = TextMuted, fontSize = 10.5.sp)
            }
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(40.dp)
                .background(PanelSurface, RoundedCornerShape(13.dp))
                .border(1.dp, LineColor, RoundedCornerShape(13.dp))
                .clickable { onSettingsClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, contentDescription = "تنظیمات", tint = TextMuted, modifier = Modifier.size(19.dp))
        }
    }
}
