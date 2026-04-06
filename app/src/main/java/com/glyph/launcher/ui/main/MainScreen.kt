package com.glyph.launcher.ui.main

import android.net.Uri
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glyph.launcher.MainActivity
import com.glyph.launcher.data.local.entity.GameEntity
import com.glyph.launcher.domain.model.Platform
import com.glyph.launcher.ui.dialog.EmulatorPickerDialog
import com.glyph.launcher.ui.dialog.RomOptionsDialog
import com.glyph.launcher.ui.main.components.BackgroundLayer
import com.glyph.launcher.ui.main.components.CoverArtBox
import com.glyph.launcher.ui.main.components.GameList
import com.glyph.launcher.ui.main.components.PlatformSelector
import com.glyph.launcher.ui.theme.*
import com.glyph.launcher.util.EmulatorLauncher

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showRomOptions by remember { mutableStateOf(false) }
    var romOptionsGame by remember { mutableStateOf<GameEntity?>(null) }
    var pendingImageGame by remember { mutableStateOf<GameEntity?>(null) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val game = pendingImageGame ?: return@rememberLauncherForActivityResult
        pendingImageGame = null
        if (uri != null) {
            try {
                val dir = File(context.filesDir, "backgrounds")
                if (!dir.exists()) dir.mkdirs()
                val ext = context.contentResolver.getType(uri)?.substringAfter("/") ?: "jpg"
                val outFile = File(dir, "bg_${game.gameId}.${if (ext == "jpeg") "jpg" else ext}")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.setGameCoverPath(game.gameId, outFile.absolutePath)
            } catch (_: Exception) { }
        }
    }

    BackHandler(enabled = showRomOptions) {
        showRomOptions = false
        romOptionsGame = null
    }
    BackHandler(enabled = !showRomOptions, onBack = onBack)

    var showEmulatorPicker by remember { mutableStateOf(false) }
    var pickerGame by remember { mutableStateOf<GameEntity?>(null) }
    var pickerPlatform by remember { mutableStateOf<Platform?>(null) }

    val listFocusRequester = remember { FocusRequester() }
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            kotlinx.coroutines.delay(150)
            listFocusRequester.requestFocus()
        }
    }

    // Register key listener; re-register after delay so when returning from Settings we always get control back.
    val activity = context as? MainActivity
    DisposableEffect(Unit) {
        activity?.setKeyEventListener { event: KeyEvent ->
            viewModel.onKeyEvent(event)
        }
        onDispose {
            activity?.setKeyEventListener(null)
        }
    }
    // Re-register after a delay so we win over Settings' onDispose when returning from Settings.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(350)
        activity?.setKeyEventListener { event: KeyEvent ->
            viewModel.onKeyEvent(event)
        }
    }

    // ── One-shot Events ──────────────────────────────────────────────────────

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainViewModel.Event.LaunchGame -> {
                    val game = event.game
                    val emulatorPkg = game.preferredEmulatorPackage ?: return@collect
                    val platform = Platform.fromTag(game.platformTag)

                    // ALWAYS verify the emulator is still installed before launching
                    if (platform != null) {
                        val detection = EmulatorLauncher.detectInstalledEmulators(context, platform)
                        val isInstalled = detection.installed.any { it.packageName == emulatorPkg }

                        if (isInstalled) {
                            EmulatorLauncher.launchGame(context, game, emulatorPkg)
                        } else {
                            // Saved emulator was uninstalled — show picker
                            pickerGame = game
                            pickerPlatform = platform
                            showEmulatorPicker = true
                        }
                    }
                }

                is MainViewModel.Event.ShowEmulatorPicker -> {
                    val game = event.game
                    val platform = event.platform

                    val detection = EmulatorLauncher.detectInstalledEmulators(context, platform)
                    when {
                        detection.installed.size == 1 -> {
                            // Only one installed — launch directly
                            val pkg = detection.installed.first().packageName
                            viewModel.onEmulatorSelected(game, pkg, true)
                        }
                        detection.installed.size > 1 -> {
                            // Multiple installed — let user choose
                            pickerGame = game
                            pickerPlatform = platform
                            showEmulatorPicker = true
                        }
                        else -> {
                            // NONE installed — show dialog with download links
                            pickerGame = game
                            pickerPlatform = platform
                            showEmulatorPicker = true
                        }
                    }
                }

                is MainViewModel.Event.ShowRomOptions -> {
                    romOptionsGame = event.game
                    showRomOptions = true
                }

                is MainViewModel.Event.NavigateToSetup -> {
                    onNavigateToSettings()
                }
                is MainViewModel.Event.GoBack -> {
                    onBack()
                }
                is MainViewModel.Event.ToggleSearch -> {
                    if (isSearchExpanded) {
                        viewModel.clearSearch()
                        isSearchExpanded = false
                    } else {
                        isSearchExpanded = true
                    }
                }
            }
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────────

    Box(modifier = Modifier.fillMaxSize()) {
        BackgroundLayer(currentGame = state.selectedGame)

        AnimatedVisibility(
            visible = state.isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            CircularProgressIndicator(color = GlyphWhiteLow)
        }

        AnimatedVisibility(
            visible = !state.isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(16.dp))
                // Platform group: ALL + platforms + SETTINGS + BACK (same chip style; no big highlight box)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                ) {
                    PlatformSelector(
                        platforms = state.platforms,
                        activePlatform = state.activePlatformFilter,
                        onPlatformSelected = { viewModel.setPlatformFilter(it) },
                        barSelectionIndex = state.barSelectionIndex,
                        onBarFocusIndex = { viewModel.setBarSelectionIndex(it) },
                        onSettingsClick = onNavigateToSettings,
                        onBackClick = onBack
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // ── Search Bar ──
                val searchFocusRequester = remember { FocusRequester() }
                val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

                // When search is activated, request focus and show keyboard
                LaunchedEffect(isSearchExpanded) {
                    if (isSearchExpanded) {
                        kotlinx.coroutines.delay(100)
                        searchFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(GlyphSurface, RoundedCornerShape(8.dp))
                        .clickable {
                            if (!isSearchExpanded) {
                                isSearchExpanded = true
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🔍",
                        style = GlyphTypography.bodyMedium.copy(color = GlyphDimmed),
                        modifier = Modifier
                            .clickable {
                                if (!isSearchExpanded) {
                                    isSearchExpanded = true
                                }
                            }
                            .padding(end = 8.dp)
                    )
                    if (isSearchExpanded) {
                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = GlyphWhiteHigh,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(GlyphAccent),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.weight(1f)) {
                                    if (state.searchQuery.isEmpty()) {
                                        Text(
                                            "Search games...",
                                            style = TextStyle(
                                                color = GlyphDimmed,
                                                fontSize = 14.sp
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(searchFocusRequester)
                        )
                        Text(
                            text = "✕",
                            style = GlyphTypography.titleMedium.copy(
                                color = GlyphWhiteMedium
                            ),
                            modifier = Modifier
                                .clickable {
                                    viewModel.clearSearch()
                                    isSearchExpanded = false
                                    keyboardController?.hide()
                                }
                                .padding(start = 8.dp)
                        )
                    } else {
                        Text(
                            "Search games...",
                            style = TextStyle(
                                color = GlyphDimmed,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(min = 280.dp)
                            .focusRequester(listFocusRequester)
                            .focusTarget()
                            .focusable()
                            .pointerInput(Unit) {
                                var totalDrag = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalDrag = 0f },
                                    onDragEnd = {
                                        if (totalDrag < -50) {
                                            viewModel.cyclePlatformSelection(true) // Next (Swipe Left)
                                        } else if (totalDrag > 50) {
                                            viewModel.cyclePlatformSelection(false) // Prev (Swipe Right)
                                        }
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDrag += dragAmount
                                    }
                                )
                            }
                    ) {
                        AnimatedContent(
                            targetState = state.games,
                            transitionSpec = {
                                val direction = state.transitionDirection
                                if (direction > 0) {
                                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                                } else if (direction < 0) {
                                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                                } else {
                                    fadeIn() togetherWith fadeOut()
                                }
                            },
                            label = "GameListAnimation"
                        ) { targetGames ->
                            GameList(
                                games = targetGames,
                                selectedIndex = state.selectedIndex,
                                onSelectIndex = { viewModel.selectIndex(it) },
                                onConfirm = { viewModel.confirmSelection() },
                                onRomOptions = {
                                    state.selectedGame?.let { romOptionsGame = it; showRomOptions = true }
                                },
                                platformLabel = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    CoverArtBox(
                        selectedGame = state.selectedGame,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 24.dp, bottom = 24.dp)
                            .width(240.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }

        if (!state.isLoading && state.games.isNotEmpty()) {
            Text(
                text = "${state.selectedIndex + 1} / ${state.games.size}",
                style = GameMetadata.copy(color = GlyphWhiteLow),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 24.dp, end = 32.dp)
            )
        }
    }

    // ── Emulator Picker Dialog ───────────────────────────────────────────────

    if (showEmulatorPicker && pickerGame != null && pickerPlatform != null) {
        EmulatorPickerDialog(
            game = pickerGame!!,
            platform = pickerPlatform!!,
            onEmulatorSelected = { game, pkg, alwaysUse ->
                showEmulatorPicker = false
                viewModel.onEmulatorSelected(game, pkg, alwaysUse)
            },
            onDismiss = { showEmulatorPicker = false }
        )
    }

    if (showRomOptions && romOptionsGame != null) {
        val game = romOptionsGame!!
        RomOptionsDialog(
            game = game,
            onRename = { g, newTitle -> viewModel.updateGameTitle(g.gameId, newTitle) },
            onRescrape = { viewModel.rescrapeGame(it) },
            onAddImage = { g ->
                pendingImageGame = g
                imagePickerLauncher.launch("image/*")
            },
            onDelete = { viewModel.deleteGame(it) },
            onChangeEmulator = { g ->
                Platform.fromTag(g.platformTag)?.let { platform ->
                    pickerGame = g
                    pickerPlatform = platform
                    showEmulatorPicker = true
                }
            },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onDismiss = { showRomOptions = false; romOptionsGame = null }
        )
    }
}
