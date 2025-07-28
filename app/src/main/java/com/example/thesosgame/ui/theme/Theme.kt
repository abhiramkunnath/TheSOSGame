package com.example.thesosgame.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ModernDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = Surface,
    primaryContainer = PrimaryVariant,
    onPrimaryContainer = Surface,
    secondary = Secondary,
    onSecondary = Surface,
    secondaryContainer = SecondaryVariant,
    onSecondaryContainer = Surface,
    tertiary = Accent,
    onTertiary = DarkBackground,
    tertiaryContainer = AccentVariant,
    onTertiaryContainer = Surface,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = Error,
    onError = Surface,
    outline = DarkOnSurfaceVariant,
    outlineVariant = DarkSurfaceVariant
)

private val ModernLightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Surface,
    primaryContainer = Primary.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryVariant,
    secondary = Secondary,
    onSecondary = Surface,
    secondaryContainer = Secondary.copy(alpha = 0.1f),
    onSecondaryContainer = SecondaryVariant,
    tertiary = Accent,
    onTertiary = Surface,
    tertiaryContainer = Accent.copy(alpha = 0.1f),
    onTertiaryContainer = AccentVariant,
    background = Background,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = Error,
    onError = Surface,
    outline = OnSurfaceVariant,
    outlineVariant = SurfaceVariant
)

@Composable
fun TheSOSgameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to use our custom theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ModernDarkColorScheme
        else -> ModernLightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}