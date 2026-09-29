package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Exact Happy Clipboard Dark Scheme according to design specification
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF82C0CC),
    onPrimary = Color(0xFF06343D),
    primaryContainer = Color(0xFF16697A),
    onPrimaryContainer = Color(0xFFD8F3F6),
    secondary = Color(0xFF489FB5),
    onSecondary = Color(0xFF062C34),
    secondaryContainer = Color(0xFF234E57),
    onSecondaryContainer = Color(0xFFD7F4F7),
    tertiary = Color(0xFFFFA62B),
    onTertiary = Color(0xFF2A1700),
    tertiaryContainer = Color(0xFF69450F),
    onTertiaryContainer = Color(0xFFFFE8C2),
    background = Color(0xFF101718),
    onBackground = Color(0xFFEDE7E3),
    surface = Color(0xFF1A2527),
    onSurface = Color(0xFFEDE7E3),
    surfaceVariant = Color(0xFF26383C),
    onSurfaceVariant = Color(0xFFC2CED0),
    outline = Color(0xFF344448),
    outlineVariant = Color(0xFF344448),
    error = Color(0xFFFF8A6B),
    onError = Color(0xFF3A0A00)
)

// Exact Happy Clipboard Light Scheme according to design specification
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF16697A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF82C0CC),
    onPrimaryContainer = Color(0xFF073B46),
    secondary = Color(0xFF489FB5),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8F0F4),
    onSecondaryContainer = Color(0xFF123A42),
    tertiary = Color(0xFFFFA62B),
    onTertiary = Color(0xFF2A1700),
    tertiaryContainer = Color(0xFFFFE0B2),
    onTertiaryContainer = Color(0xFF3A2100),
    background = Color(0xFFF8F7F5),
    onBackground = Color(0xFF162528),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF162528),
    surfaceVariant = Color(0xFFE4DFDA),
    onSurfaceVariant = Color(0xFF4A4643),
    outline = Color(0xFFD8D2CA),
    outlineVariant = Color(0xFFD8D2CA),
    error = Color(0xFFD95D39),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "SYSTEM",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode.uppercase()) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        androidx.compose.material3.Surface(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MyApplicationTheme(
        themeMode = if (darkTheme) "DARK" else "LIGHT",
        dynamicColor = dynamicColor,
        content = content
    )
}
