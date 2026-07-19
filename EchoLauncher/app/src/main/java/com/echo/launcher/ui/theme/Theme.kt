package com.echo.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EchoColorScheme = darkColorScheme(
    background = Bg,
    surface = Surface,
    primary = Violet,
    secondary = Cyan,
    onBackground = Ink,
    onSurface = Ink,
    error = Danger
)

@Composable
fun EchoLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EchoColorScheme,
        content = content
    )
}
