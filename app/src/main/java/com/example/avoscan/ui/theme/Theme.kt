package com.example.avoscan.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary           = GreenPrimary,
    onPrimary         = SurfaceWhite,
    primaryContainer  = GreenPrimaryLight,
    onPrimaryContainer = SurfaceWhite,

    secondary         = GreenAccent,
    onSecondary       = SurfaceWhite,

    background        = BackgroundLight,
    onBackground      = TextPrimary,

    surface           = SurfaceWhite,
    onSurface         = TextPrimary,
    surfaceVariant    = SurfaceGray,
    onSurfaceVariant  = TextSecondary,

    error             = SeverityHigh,
    onError           = SurfaceWhite,

    outline           = BorderLight,
    outlineVariant    = BorderLight
)

private val DarkColors = darkColorScheme(
    primary           = GreenPrimaryLight,
    onPrimary         = SurfaceDark,
    background        = BackgroundDark,
    onBackground      = TextPrimaryDark,
    surface           = SurfaceDark,
    onSurface         = TextPrimaryDark,
    surfaceVariant    = SurfaceDark,
    onSurfaceVariant  = TextSecondaryDark
)

@Composable
fun AvoScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AvoScanTypography,
        content     = content
    )
}