package com.arka.vpn.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arka.vpn.ui.theme.LineColor
import com.arka.vpn.ui.theme.PanelSurface
import com.arka.vpn.ui.theme.TextMuted
import com.arka.vpn.ui.theme.TextPrimary

@Composable
fun SpeedGraphCard(speedLabel: String, values: List<Float>, lineColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelSurface, RoundedCornerShape(19.dp))
            .border(1.dp, LineColor, RoundedCornerShape(19.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("سرعت لحظه‌ای", color = TextMuted, fontSize = 11.sp)
            Text(speedLabel, color = TextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (values.size < 2) return@Canvas
            val w = size.width
            val h = size.height
            val stepX = w / (values.size - 1)
            val linePath = Path()
            values.forEachIndexed { i, v ->
                val y = h - 5f - (v.coerceIn(0f, 100f) / 100f) * (h - 12f)
                val x = i * stepX
                if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.18f), Color.Transparent))
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
