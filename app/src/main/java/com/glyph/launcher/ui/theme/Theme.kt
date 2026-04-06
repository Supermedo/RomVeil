package com.glyph.launcher.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GlyphDarkColorScheme = darkColorScheme(
    primary = GlyphWhiteHigh,
    onPrimary = GlyphBlack,
    secondary = GlyphAccent,
    onSecondary = GlyphBlack,
    tertiary = GlyphDimmed,
    background = GlyphBlack,
    onBackground = GlyphWhiteHigh,
    surface = GlyphSurface,
    onSurface = GlyphWhiteHigh,
    surfaceVariant = GlyphDarkSurface,
    onSurfaceVariant = GlyphWhiteMedium,
    outline = GlyphWhiteLow,
)

@Composable
fun GlyphTheme(content: @Composable () -> Unit) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GlyphBlack.toArgb()
            window.navigationBarColor = GlyphBlack.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = GlyphDarkColorScheme,
        typography = GlyphTypography,
        content = content
    )
}
