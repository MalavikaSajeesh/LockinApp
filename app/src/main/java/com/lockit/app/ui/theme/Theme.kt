package com.lockit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Core palette: black, white, grey, red (locked/incomplete), green (unlocked/complete)
val LockItBlack = Color(0xFF0E0E0E)
val LockItDarkGrey = Color(0xFF2A2A2A)
val LockItGrey = Color(0xFF8A8A8A)
val LockItLightGrey = Color(0xFFE3E3E3)
val LockItWhite = Color(0xFFFFFFFF)
val LockItRed = Color(0xFFE63946)
val LockItRedDark = Color(0xFFB22C36)
val LockItGreen = Color(0xFF2ECC71)
val LockItGreenDark = Color(0xFF219150)

private val LockItColorScheme = darkColorScheme(
    primary = LockItRed,
    onPrimary = LockItWhite,
    secondary = LockItGreen,
    onSecondary = LockItBlack,
    background = LockItBlack,
    onBackground = LockItLightGrey,
    surface = LockItDarkGrey,
    onSurface = LockItLightGrey,
    error = LockItRed,
    onError = LockItWhite,
    surfaceVariant = LockItDarkGrey,
    onSurfaceVariant = LockItGrey
)

@Composable
fun LockItTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LockItColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
