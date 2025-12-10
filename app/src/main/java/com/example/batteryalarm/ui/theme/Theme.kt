package com.example.batteryalarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF81C784),
    tertiary = Color(0xFFFFC107),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    error = Color(0xFFFF6B6B)
)

@Composable
fun BatteryAlarmTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
