package com.glyph.launcher.ui.main.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.ui.theme.GameMetadata
import com.glyph.launcher.ui.theme.GlyphWhiteLow
import kotlin.math.abs

// ── Item height per proximity tier ───────────────────────────────────────────
private val SELECTED_HEIGHT = 110.dp
private val NEAR_HEIGHT = 58.dp
private val FAR_HEIGHT = 52.dp

private fun heightForDistance(distance: Int): Dp = when (distance) {
    0 -> SELECTED_HEIGHT
    1 -> NEAR_HEIGHT
    else -> FAR_HEIGHT
}

/**
 * Vertical game list using LazyColumn so only visible items are composed.
 * Fixes text cutoff with 10+ ROMs. Selection is driven by controller/drag; list scrolls to keep selected item in view.
 */
@Composable
fun GameList(
    games: List<GameEntity>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onConfirm: () -> Unit,
    onRomOptions: (() -> Unit)? = null,
    platformLabel: String?,
    modifier: Modifier = Modifier
) {
    if (games.isEmpty()) {
        EmptyState(modifier)
        return
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (games.size - 1).coerceAtLeast(0))
    )
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { NEAR_HEIGHT.toPx() * 0.6f }
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }
    var pendingIndexDelta by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedIndex) {
        val index = selectedIndex.coerceIn(0, games.size - 1)
        listState.animateScrollToItem(index)
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(games.size, selectedIndex) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            accumulatedDrag = 0f
                            pendingIndexDelta = 0
                        },
                        onVerticalDrag = { _, dragAmount ->
                            accumulatedDrag += dragAmount
                            val steps = -(accumulatedDrag / swipeThresholdPx).toInt()
                            if (steps != pendingIndexDelta) {
                                val delta = steps - pendingIndexDelta
                                pendingIndexDelta = steps
                                val newIndex = (selectedIndex + delta).coerceIn(0, games.size - 1)
                                onSelectIndex(newIndex)
                            }
                        },
                        onDragEnd = {
                            accumulatedDrag = 0f
                            pendingIndexDelta = 0
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onConfirm() },
                        onLongPress = { onRomOptions?.invoke() }
                    )
                },
            userScrollEnabled = false
        ) {
            itemsIndexed(
                items = games,
                key = { _, game -> game.gameId }
            ) { index, game ->
                val distance = abs(index - selectedIndex)
                val proximity = when (distance) {
                    0 -> ItemProximity.SELECTED
                    1 -> ItemProximity.NEAR
                    else -> ItemProximity.FAR
                }
                val itemHeight = heightForDistance(distance)
                Box(modifier = Modifier.fillMaxWidth().height(itemHeight)) {
                    GameListItem(
                        game = game,
                        proximity = proximity,
                        showMetadata = distance == 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (platformLabel != null) {
            Text(
                text = platformLabel.uppercase(),
                style = GameMetadata.copy(color = GlyphWhiteLow),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 32.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "NO GAMES FOUND",
                style = GameMetadata.copy(color = GlyphWhiteLow)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Press START to open settings",
                style = GameMetadata.copy(color = GlyphWhiteLow)
            )
        }
    }
}
