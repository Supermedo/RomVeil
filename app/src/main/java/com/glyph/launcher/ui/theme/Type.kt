package com.glyph.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.glyph.launcher.R

/**
 * Glyph Typography System
 *
 * Uses Inter for a clean, modern sans-serif feel.
 * The font hierarchy is designed for the center-snap list:
 *   - SELECTED:    Large, bold, full white — the "hero" item
 *   - NEAR:        Medium, semi-bold, faded — items ±1 from center
 *   - FAR:         Small, regular, very faded — items ±2+ from center
 *   - METADATA:    Tiny, light — year/platform/developer under selected
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_light, FontWeight.Light),
)

// ── Game List Item Styles ────────────────────────────────────────────────────

/**
 * The focused/highlighted game title — big, bold, fully opaque.
 */
val GameTitleSelected = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 42.sp,
    letterSpacing = (-0.5).sp,
    lineHeight = 48.sp,
    color = GlyphWhiteHigh
)

/**
 * Items ±1 from center — slightly smaller, less prominent.
 */
val GameTitleNear = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    letterSpacing = 0.sp,
    lineHeight = 34.sp,
    color = GlyphWhiteMedium
)

/**
 * Items ±2+ from center — smallest, most faded.
 */
val GameTitleFar = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 20.sp,
    letterSpacing = 0.5.sp,
    lineHeight = 26.sp,
    color = GlyphWhiteLow
)

/**
 * Metadata line under selected item: "1990 · Nintendo · Platformer"
 */
val GameMetadata = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Light,
    fontSize = 14.sp,
    letterSpacing = 1.5.sp,
    lineHeight = 20.sp,
    color = GlyphDimmed
)

/**
 * Platform tag (e.g., "SNES") shown as a label.
 */
val PlatformLabel = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 2.sp,
    lineHeight = 14.sp,
    color = GlyphWhiteMedium
)

// ── Material 3 Typography ────────────────────────────────────────────────────

val GlyphTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.5.sp,
    ),
)
