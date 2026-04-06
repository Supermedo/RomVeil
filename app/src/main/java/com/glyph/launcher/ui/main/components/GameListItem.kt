package com.glyph.launcher.ui.main.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.domain.model.Platform
import com.glyph.launcher.ui.theme.*

/**
 * Visual state of a game list item based on its distance from the center.
 */
enum class ItemProximity {
    SELECTED,   // distance = 0 (center)
    NEAR,       // distance = 1
    FAR         // distance >= 2
}

/**
 * A single game item in the vertical list.
 *
 * The item's visual weight (size, opacity, font weight) is determined by
 * its proximity to the center-selected position, creating a "dial" effect.
 */
@Composable
fun GameListItem(
    game: GameEntity,
    proximity: ItemProximity,
    showMetadata: Boolean = false,
    modifier: Modifier = Modifier
) {
    val animSpec = tween<Float>(durationMillis = 200)
    val colorAnimSpec = tween<androidx.compose.ui.graphics.Color>(durationMillis = 200)

    // Only the selected item is prominent; NEAR/FAR are clearly secondary (no confusion)
    val targetFontSize = when (proximity) {
        ItemProximity.SELECTED -> 40f
        ItemProximity.NEAR -> 22f
        ItemProximity.FAR -> 18f
    }
    val fontSize by animateFloatAsState(
        targetValue = targetFontSize,
        animationSpec = animSpec,
        label = "item_font_size"
    )

    // Selected = full emphasis; others clearly dimmed so only one looks "chosen"
    val targetColor = when (proximity) {
        ItemProximity.SELECTED -> GlyphWhiteHigh
        ItemProximity.NEAR -> Color.White.copy(alpha = 0.5f)
        ItemProximity.FAR -> Color.White.copy(alpha = 0.3f)
    }
    val textColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = colorAnimSpec,
        label = "item_color"
    )

    val fontWeight = when (proximity) {
        ItemProximity.SELECTED -> FontWeight.Bold
        ItemProximity.NEAR -> FontWeight.Normal
        ItemProximity.FAR -> FontWeight.Normal
    }

    val letterSpacing = when (proximity) {
        ItemProximity.SELECTED -> (-0.5).sp
        ItemProximity.NEAR -> 0.sp
        ItemProximity.FAR -> 0.5.sp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (game.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = if (proximity == ItemProximity.SELECTED) GlyphAccent else textColor, // Use accent when selected
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(if (proximity == ItemProximity.SELECTED) 24.dp else 14.dp)
                    )
                }
                Text(
                    text = game.displayTitle,
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = fontWeight,
                        fontSize = fontSize.sp,
                        letterSpacing = letterSpacing,
                        color = textColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Metadata line — only visible for the selected item
            if (showMetadata && proximity == ItemProximity.SELECTED) {
                Spacer(modifier = Modifier.height(4.dp))

                val metadataParts = buildList {
                    game.releaseDate?.let { add(it) }
                    val platformName = Platform.fromTag(game.platformTag)?.displayName
                        ?: game.platformTag.uppercase()
                    add(platformName)
                    game.rating?.let { r ->
                        add("%.1f ★".format(r.coerceIn(0f, 5f)))
                    }
                    game.developer?.let { add(it) }
                    game.genre?.let { add(it) }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = metadataParts.joinToString("  ·  ").uppercase(),
                        style = GameMetadata,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}
