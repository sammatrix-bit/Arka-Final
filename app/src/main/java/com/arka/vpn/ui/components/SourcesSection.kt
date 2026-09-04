package com.arka.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arka.vpn.ui.theme.AccentGreen
import com.arka.vpn.ui.theme.LineColor
import com.arka.vpn.ui.theme.PanelSurface
import com.arka.vpn.ui.theme.PanelSurface2
import com.arka.vpn.ui.theme.TextMuted
import com.arka.vpn.ui.theme.TextPrimary

@Composable
fun SourcesSection(
    selectedSource: String,
    privateUnlocked: Boolean,
    onSelectSource: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelSurface, RoundedCornerShape(19.dp))
            .border(1.dp, LineColor, RoundedCornerShape(19.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("منبع اتصال", color = TextMuted, fontSize = 10.5.sp)
            Text("یکی از مسیرها را انتخاب کن", color = TextMuted, fontSize = 10.5.sp)
        }
        SourceRow(
            icon = Icons.Default.Public,
            title = "عمومی",
            subtitle = "دسترسی آزاد برای همه",
            badgeText = "فعال",
            badgeOk = true,
            selected = selectedSource == "public",
            onClick = { onSelectSource("public") }
        )
        SourceRow(
            icon = if (privateUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
            title = "شخصی",
            subtitle = if (privateUnlocked) "مسیر شخصی شما آماده است" else "فعال‌سازی با لینک اختصاصی",
            badgeText = if (privateUnlocked) "فعال" else "قفل",
            badgeOk = privateUnlocked,
            selected = selectedSource == "private",
            onClick = { onSelectSource("private") },
            iconTint = if (privateUnlocked) AccentGreen else TextMuted
        )
    }
}

@Composable
private fun SourceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeOk: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    iconTint: Color = TextMuted
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0x176C7BFF) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(PanelSurface2, RoundedCornerShape(12.dp))
                .border(1.dp, LineColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 13.sp)
            Text(subtitle, color = TextMuted, fontSize = 10.5.sp)
        }
        Text(
            badgeText,
            color = if (badgeOk) AccentGreen else TextMuted,
            fontSize = 9.5.sp,
            modifier = Modifier
                .background(if (badgeOk) Color(0x1238E8B0) else PanelSurface2, RoundedCornerShape(99.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) Color(0xFF6C7BFF) else Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF6C7BFF)))
            }
        }
    }
}

@Composable
fun SoonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .border(1.dp, LineColor, RoundedCornerShape(19.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("روش‌های حرفه‌ای شرایط سخت", color = TextPrimary, fontSize = 13.sp)
            Text("الگوهای اتصال پیشرفته برای نتایج بهتر", color = TextMuted, fontSize = 10.5.sp)
        }
        Text(
            "به‌زودی",
            color = Color(0xFF9AA6FF),
            fontSize = 9.5.sp,
            modifier = Modifier
                .background(Color(0x146C7BFF), RoundedCornerShape(99.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}
