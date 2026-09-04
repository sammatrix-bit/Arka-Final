package com.arka.vpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ArkaColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentGreen,
    background = BgDark,
    surface = PanelBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = AccentRed
)

@Composable
fun ArkaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ArkaColorScheme,
        typography = ArkaTypography,
        content = content
    )
}
