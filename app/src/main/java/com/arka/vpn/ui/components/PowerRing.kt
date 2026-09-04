package com.arka.vpn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arka.vpn.model.ConnectionState
import com.arka.vpn.ui.theme.AccentAmber
import com.arka.vpn.ui.theme.AccentGreen
import com.arka.vpn.ui.theme.TextMuted

@Composable
fun PowerRing(
    state: ConnectionState,
    progress: Float,
    onTap: () -> Unit
) {
    val liveColor = when (state) {
        ConnectionState.CONNECTED -> AccentGreen
        ConnectionState.CONNECTING -> AccentAmber
        ConnectionState.IDLE -> Color(0xFF6C7BFF)
    }
    val label = when (state) {
        ConnectionState.IDLE -> "اتصال"
        ConnectionState.CONNECTING -> "صبر کنید…"
        ConnectionState.CONNECTED -> "قطع"
    }

    Box(modifier = Modifier.size(226.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeTrack = 2.dp.toPx()
            val strokeProg = 3.dp.toPx()
            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeTrack, cap = StrokeCap.Round)
            )
            drawArc(
                color = liveColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = strokeProg, cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(168.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF222A45), Color(0xFF10141F))
                    )
                )
                .clickable { onTap() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = label,
                    tint = liveColor,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    label,
                    color = if (state == ConnectionState.CONNECTED) AccentGreen else TextMuted,
                    fontSize = 13.5.sp
                )
            }
        }
    }
}
