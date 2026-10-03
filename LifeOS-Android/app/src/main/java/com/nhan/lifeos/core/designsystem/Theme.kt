package com.nhan.lifeos.core.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = LifeOSPrimary,
    onPrimary = LifeOSTextHigh,
    primaryContainer = LifeOSSurfaceCard,
    onPrimaryContainer = LifeOSTextHigh,
    secondary = LifeOSCyan,
    onSecondary = LifeOSBackgroundDark,
    background = LifeOSBackgroundDark,
    onBackground = LifeOSTextHigh,
    surface = LifeOSSurfaceDark,
    onSurface = LifeOSTextHigh,
    surfaceVariant = LifeOSSurfaceCard,
    onSurfaceVariant = LifeOSTextMid,
    outline = LifeOSGlassBorder
)

@Composable
fun LifeOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LifeOSTypography,
        content = content
    )
}
