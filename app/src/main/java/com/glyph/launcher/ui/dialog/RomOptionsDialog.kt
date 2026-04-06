package com.glyph.launcher.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.ui.theme.GlyphDarkSurface
import com.glyph.launcher.ui.theme.GlyphWhiteHigh
import com.glyph.launcher.ui.theme.GlyphWhiteLow
import com.glyph.launcher.ui.theme.GameMetadata
import com.glyph.launcher.ui.theme.PlatformLabel
import com.glyph.launcher.ui.theme.WindowSizeClass
import com.glyph.launcher.ui.theme.rememberWindowSizeClass

/**
 * ROM context menu: rescrape cover, add image manually, delete game, change emulator.
 * Opened by Y button or long-press on game.
 */
@Composable
fun RomOptionsDialog(
    game: GameEntity,
    onRename: (GameEntity, String) -> Unit,
    onRescrape: (GameEntity) -> Unit,
    onAddImage: (GameEntity) -> Unit,
    onDelete: (GameEntity) -> Unit,
    onChangeEmulator: (GameEntity) -> Unit,
    onToggleFavorite: (GameEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var showRename by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showRename) {
        RenameDialog(
            initialName = game.displayTitle,
            onConfirm = { newName ->
                onRename(game, newName)
                onDismiss()
            },
            onDismiss = { showRename = false }
        )
    } else {
        val isCompact = rememberWindowSizeClass() == WindowSizeClass.COMPACT
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = if (isCompact) 16.dp else 48.dp)
                    .fillMaxWidth(if (isCompact) 1f else 0.5f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlyphDarkSurface)
                    .padding(24.dp)
            ) {
                Text(
                    text = "ROM OPTIONS",
                    style = PlatformLabel.copy(color = GlyphWhiteHigh)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = game.displayTitle,
                    style = GameMetadata.copy(color = GlyphWhiteLow),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(20.dp))

                OptionRow(
                    text = if (game.isFavorite) "Remove from Favorites" else "Add to Favorites",
                    onClick = { onToggleFavorite(game); onDismiss() }
                )
                OptionRow(text = "Rename game", onClick = { showRename = true })
                OptionRow(text = "Rescrape cover", onClick = { onRescrape(game); onDismiss() })
                OptionRow(text = "Add image manually", onClick = { onAddImage(game); onDismiss() })
                OptionRow(text = "Change emulator", onClick = { onChangeEmulator(game); onDismiss() })
                OptionRow(text = "Delete game", onClick = { onDelete(game); onDismiss() })

                Spacer(modifier = Modifier.height(8.dp))
                OptionRow(text = "Cancel", onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun RenameDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialName) }
    
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(GlyphDarkSurface, RoundedCornerShape(16.dp))
                .padding(24.dp)
                .fillMaxWidth(0.8f)
        ) {
            Text("RENAME GAME", style = PlatformLabel.copy(color = GlyphWhiteHigh))
            Spacer(modifier = Modifier.height(16.dp))
            
            androidx.compose.material3.TextField(
                value = text,
                onValueChange = { text = it },
                textStyle = GameMetadata.copy(color = GlyphWhiteHigh),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("CANCEL", style = PlatformLabel.copy(color = GlyphWhiteLow))
                }
                androidx.compose.material3.TextButton(onClick = { onConfirm(text) }) {
                    Text("SAVE", style = PlatformLabel.copy(color = com.glyph.launcher.ui.theme.GlyphAccent))
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = GameMetadata.copy(color = GlyphWhiteHigh),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .focusable()
            .padding(vertical = 12.dp, horizontal = 8.dp)
    )
}
