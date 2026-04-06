package com.glyph.launcher.ui.main.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.ui.theme.GlyphOverlay
import java.io.File

/**
 * Layer 1: The full-screen blurred background image.
 *
 * - Loads the game's cached background image (fanart/screenshot).
 * - Applies a heavy Gaussian blur (32dp).
 * - Overlays a 40% black scrim for text readability.
 * - Cross-fades between images when the selection changes (~300ms).
 */
@Composable
fun BackgroundLayer(
    currentGame: GameEntity?,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Cross-fade keyed by the image path (changes trigger transition)
        Crossfade(
            targetState = currentGame?.backgroundImagePath,
            animationSpec = tween(durationMillis = 300),
            label = "bg_crossfade"
        ) { imagePath ->
            if (imagePath != null) {
                val context = LocalContext.current
                val file = remember(imagePath) { File(imagePath) }
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(file)
                        .crossfade(false) // we handle our own crossfade
                        .build()
                )

                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 32.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Solid black fallback when no image is available
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black)
                )
            }
        }

        // Semi-transparent overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GlyphOverlay)
        )
    }
}
