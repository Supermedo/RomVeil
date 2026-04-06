package com.glyph.launcher.ui.main.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.foundation.focusable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.glyph.launcher.domain.model.Platform
import com.glyph.launcher.ui.theme.*
import com.glyph.launcher.ui.main.FILTER_FAVORITES

/**
 * Horizontal scrollable bar: ALL + platforms + SETTINGS + BACK.
 * L1/R1 cycles through all chips; A confirms (platform = list, SETTINGS/BACK = action).
 */
@Composable
fun PlatformSelector(
    platforms: List<String>,
    activePlatform: String?,
    onPlatformSelected: (String?) -> Unit,
    barSelectionIndex: Int, // 0=ALL, 1..n=platform, n+1=SETTINGS, n+2=BACK
    onBarFocusIndex: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (platforms.isNotEmpty()) {
            PlatformChip(
                label = "ALL",
                barIndex = 0,
                isActive = barSelectionIndex == 0,
                onFocusIndex = onBarFocusIndex,
                onClick = { onPlatformSelected(null) }
            )
            PlatformChip(
                label = "FAVORITES",
                barIndex = 1,
                isActive = barSelectionIndex == 1, // MainViewModel maps FAV to index 1
                onFocusIndex = onBarFocusIndex,
                onClick = { onPlatformSelected(FILTER_FAVORITES) }
            )
        }

        platforms.forEachIndexed { i, tag ->
            val platform = Platform.fromTag(tag)
            val label = platform?.let { shortLabel(it) } ?: tag.uppercase()
            PlatformChip(
                label = label,
                barIndex = i + 2, // Shifted by 2 (ALL + FAV)
                isActive = barSelectionIndex == i + 2,
                onFocusIndex = onBarFocusIndex,
                onClick = { onPlatformSelected(tag) }
            )
        }

        val settingsIndex = if (platforms.isEmpty()) 0 else platforms.size + 2
        val backIndex = if (platforms.isEmpty()) 1 else platforms.size + 3
        PlatformChip(
            label = "SETTINGS",
            barIndex = settingsIndex,
            isActive = barSelectionIndex == settingsIndex,
            onFocusIndex = onBarFocusIndex,
            onClick = onSettingsClick
        )
        PlatformChip(
            label = "BACK",
            barIndex = backIndex,
            isActive = barSelectionIndex == backIndex,
            onFocusIndex = onBarFocusIndex,
            onClick = onBackClick
        )
    }
}

@Composable
private fun PlatformChip(
    label: String,
    barIndex: Int,
    isActive: Boolean,
    onFocusIndex: (Int) -> Unit,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }
    val highlighted = isActive || hasFocus

    val bgColor by animateColorAsState(
        targetValue = if (highlighted) GlyphWhite else GlyphWhiteDisabled,
        animationSpec = tween(150),
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (highlighted) GlyphBlack else GlyphWhiteMedium,
        animationSpec = tween(150),
        label = "chip_text"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .focusable()
            .onFocusChanged {
                hasFocus = it.hasFocus
                if (it.hasFocus) onFocusIndex(barIndex)
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = PlatformLabel.copy(
                color = textColor,
                letterSpacing = PlatformLabel.letterSpacing,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium
            )
        )
    }
}

/**
 * Short display labels for the chip bar so they don't take too much space.
 */
private fun shortLabel(platform: Platform): String = when (platform) {
    Platform.NES -> "NES"
    Platform.SNES -> "SNES"
    Platform.N64 -> "N64"
    Platform.GB -> "GB"
    Platform.GBC -> "GBC"
    Platform.GBA -> "GBA"
    Platform.NDS -> "NDS"
    Platform.N3DS -> "3DS"
    Platform.SWITCH -> "SWITCH"
    Platform.GENESIS -> "GENESIS"
    Platform.SEGA32X -> "32X"
    Platform.SATURN -> "SATURN"
    Platform.DREAMCAST -> "DC"
    Platform.PSX -> "PS1"
    Platform.PS2 -> "PS2"
    Platform.PSP -> "PSP"
    Platform.GAMECUBE -> "GCN"
    Platform.WII -> "WII"
    Platform.NEOGEO -> "NEO"
    Platform.ARCADE -> "ARCADE"
}
