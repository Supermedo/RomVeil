package com.glyph.launcher.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.domain.model.EmulatorInfo
import com.glyph.launcher.domain.model.Platform
import com.glyph.launcher.ui.theme.*
import com.glyph.launcher.util.EmulatorLauncher
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color

/**
 * Dialog shown when launching a game.
 * - If emulators are installed: lets user pick one.
 * - If none installed: shows download links to Play Store.
 */
@Composable
fun EmulatorPickerDialog(
    game: GameEntity,
    platform: Platform,
    onEmulatorSelected: (GameEntity, String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val detected = remember(platform) {
        EmulatorLauncher.detectInstalledEmulators(context, platform)
    }

    var alwaysUse by remember { mutableStateOf(true) }

    val isCompact = rememberWindowSizeClass() == WindowSizeClass.COMPACT

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = if (isCompact) 16.dp else 48.dp)
                .fillMaxWidth(if (isCompact) 1f else 0.55f)
                .background(
                    color = GlyphDarkSurface,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            Text(
                text = "CHOOSE EMULATOR",
                style = PlatformLabel.copy(color = GlyphWhiteMedium)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = platform.displayName,
                style = GameMetadata.copy(color = GlyphDimmed)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Installed Emulators ──────────────────────────────────────────
            if (detected.installed.isNotEmpty()) {
                Text(
                    text = "INSTALLED",
                    style = PlatformLabel.copy(color = GlyphDimmed)
                )
                Spacer(modifier = Modifier.height(8.dp))

                detected.installed.forEach { emulator ->
                    EmulatorOption(
                        emulator = emulator,
                        actionLabel = "LAUNCH",
                        onClick = {
                            onEmulatorSelected(game, emulator.packageName, alwaysUse)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // "Always use this" checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { alwaysUse = !alwaysUse }
                ) {
                    Checkbox(
                        checked = alwaysUse,
                        onCheckedChange = { alwaysUse = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = GlyphWhiteHigh,
                            uncheckedColor = GlyphDimmed,
                            checkmarkColor = GlyphBlack
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Always use this for ${platform.displayName}",
                        style = GameMetadata.copy(color = GlyphWhiteMedium)
                    )
                }
            }

            // ── Not Installed — Play Store Downloads ─────────────────────────
            if (detected.notInstalled.isNotEmpty()) {
                if (detected.installed.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = if (detected.installed.isEmpty()) "NO EMULATORS FOUND — DOWNLOAD ONE"
                    else "MORE EMULATORS",
                    style = PlatformLabel.copy(color = GlyphDimmed)
                )
                Spacer(modifier = Modifier.height(8.dp))

                detected.notInstalled.forEach { emulator ->
                    val label = if (emulator.downloadUrl != null) "DOWNLOAD" else "GET"
                    EmulatorOption(
                        emulator = emulator,
                        actionLabel = label,
                        isDownload = true,
                        onClick = {
                            EmulatorLauncher.openDownloadPage(context, emulator)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun EmulatorOption(
    emulator: EmulatorInfo,
    actionLabel: String,
    isDownload: Boolean = false,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) GlyphWhiteHigh else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isFocused) GlyphSurface.copy(alpha = 0.8f) 
                        else if (isDownload) GlyphBlack.copy(alpha = 0.4f) else GlyphSurface,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = emulator.displayName,
            style = GameTitleFar.copy(
                color = if (isFocused) GlyphWhiteHigh else if (isDownload) GlyphWhiteMedium else GlyphWhiteHigh
            )
        )
        Text(
            text = actionLabel,
            style = PlatformLabel.copy(
                color = if (isFocused) GlyphAccent else if (isDownload) GlyphAccent else GlyphDimmed
            )
        )
    }
}
