package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenLight,
    onPrimaryContainer = BrandGreenDark,
    secondary = ElectricAmber,
    onSecondary = Color.White,
    secondaryContainer = ElectricAmberLight,
    onSecondaryContainer = Color(0xFF7C4A00),
    tertiary = BlueInfo,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = CharcoalDark,
    surface = Color.White,
    onSurface = CharcoalDark,
    surfaceVariant = LightSlate,
    onSurfaceVariant = SlateGray,
    outline = CardBorder
)

@Composable
fun ERide3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BrandGreen.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
