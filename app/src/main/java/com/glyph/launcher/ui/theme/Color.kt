package com.glyph.launcher.ui.theme

import androidx.compose.ui.graphics.Color

// ── Glyph Palette ────────────────────────────────────────────────────────────
// A minimalist dark palette designed for high-contrast typography on blurred backgrounds.

val GlyphBlack = Color(0xFF000000)
val GlyphDarkSurface = Color(0xFF0A0A0A)
val GlyphSurface = Color(0xFF121212)
val GlyphOverlay = Color(0x66000000)            // 40% black for background overlay

val GlyphWhite = Color(0xFFFFFFFF)
val GlyphWhiteHigh = Color(0xFFF0F0F0)          // High-emphasis text
val GlyphWhiteMedium = Color(0xB3FFFFFF)         // 70% white — secondary text
val GlyphWhiteLow = Color(0x4DFFFFFF)            // 30% white — deselected items
val GlyphWhiteDisabled = Color(0x1AFFFFFF)       // 10% white — very subtle

val GlyphAccent = Color(0xFFE0E0E0)             // Soft accent for subtle highlights
val GlyphDimmed = Color(0xFF666666)              // Metadata text
/** Bright focus ring so controller selection is obvious on TV/gamepad */
val GlyphFocusRing = Color(0xFF00D4FF)          // Cyan

// Platform badge colors (optional, for subtle platform indicators)
val PlatformNES     = Color(0xFFB71C1C)
val PlatformSNES    = Color(0xFF6A1B9A)
val PlatformN64     = Color(0xFF1B5E20)
val PlatformGB      = Color(0xFF004D40)
val PlatformGBA     = Color(0xFF311B92)
val PlatformNDS     = Color(0xFF0D47A1)
val PlatformGenesis = Color(0xFFBF360C)
val PlatformPSX     = Color(0xFF1A237E)
val PlatformPSP     = Color(0xFF263238)
val PlatformNeoGeo  = Color(0xFFFFB300)  // Neo Geo gold
val PlatformArcade  = Color(0xFFF57F17)
