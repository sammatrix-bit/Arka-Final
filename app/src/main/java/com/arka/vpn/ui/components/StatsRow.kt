package com.arka.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arka.vpn.ui.theme.LineColor
import com.arka.vpn.ui.theme.PanelSurface
import com.arka.vpn.ui.theme.TextMuted
import com.arka.vpn.ui.theme.TextPrimary

@Composable
fun StatsRow(pingLabel: String, timeLabel: String, dataLabel: String, pingColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatBox("تاخیر", pingLabel, pingColor, Modifier.weight(1f))
        StatBox("مدت اتصال", timeLabel, TextPrimary, Modifier.weight(1f))
        StatBox("داده", dataLabel, TextPrimary, Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PanelSurface, RoundedCornerShape(17.dp))
            .border(1.dp, LineColor, RoundedCornerShape(17.dp))
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = TextMuted, fontSize = 9.5.sp)
        Spacer(modifier = Modifier.height(5.dp))
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
