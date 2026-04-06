package com.glyph.launcher.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Simple window size classification for adaptive layouts.
 *
 * - COMPACT:  phones (either portrait with < 600dp width, OR landscape with < 500dp height)
 * - MEDIUM:   small tablets or phones with enough room (600..839dp width AND >= 500dp height)
 * - EXPANDED: tablets, desktops (≥ 840dp width AND >= 500dp height)
 *
 * The key insight: a phone in landscape has wide width but very little height,
 * so it should still be treated as COMPACT to avoid horizontal Row layouts
 * that push buttons off-screen vertically.
 */
enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }

/**
 * Returns the current window size class based on BOTH screen width and height.
 * A phone in landscape with limited height (< 500dp) is treated as COMPACT
 * so that vertical stacked layouts are used instead of side-by-side rows.
 */
@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        val w = configuration.screenWidthDp
        val h = configuration.screenHeightDp
        when {
            w < 600 || h < 500 -> WindowSizeClass.COMPACT
            w < 840 -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }
    }
}

/**
 * True when the device is in landscape orientation.
 */
@Composable
fun isLandscape(): Boolean {
    return LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
}
