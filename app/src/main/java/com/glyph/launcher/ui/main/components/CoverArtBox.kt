package com.glyph.launcher.ui.main.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.ui.theme.GameMetadata
import com.glyph.launcher.ui.theme.GlyphDarkSurface
import com.glyph.launcher.ui.theme.GlyphSurface
import com.glyph.launcher.ui.theme.GlyphWhiteLow
import com.glyph.launcher.ui.theme.GlyphWhiteMedium
import java.io.File

/**
 * Shows the selected game's cover/artwork in a visible box (right side of main screen).
 * Uses the scraped background image; if none, shows a "NO COVER" placeholder.
 */
@Composable
fun CoverArtBox(
    selectedGame: GameEntity?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .background(GlyphDarkSurface, shape)
            .border(1.dp, GlyphWhiteMedium.copy(alpha = 0.4f), shape)
    ) {
        val imagePath = selectedGame?.backgroundImagePath
        if (imagePath != null) {
            val context = LocalContext.current
            val file = remember(imagePath) { File(imagePath) }
            // Include lastModified so rescrape (overwrites same path) triggers new load
            val model = remember(imagePath, file.lastModified()) {
                ImageRequest.Builder(context).data(file).build()
            }
            val painter = rememberAsyncImagePainter(model = model)
            Image(
                painter = painter,
                contentDescription = "Cover art for ${selectedGame.displayTitle}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GlyphSurface, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO COVER",
                    style = GameMetadata.copy(color = GlyphWhiteMedium)
                )
            }
        }
        // Star rating overlay (0–5)
        selectedGame?.rating?.let { r ->
            val ratingStr = "%.1f ★".format(r.coerceIn(0f, 5f))
            Text(
                text = ratingStr,
                style = GameMetadata.copy(color = GlyphWhiteMedium),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }
}
