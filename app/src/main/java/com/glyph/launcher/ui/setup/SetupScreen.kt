package com.glyph.launcher.ui.setup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.glyph.launcher.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.glyph.launcher.domain.model.EmulatorInfo
import com.glyph.launcher.domain.model.Platform
import com.glyph.launcher.ui.theme.GlyphTypography
import com.glyph.launcher.ui.theme.*
import com.glyph.launcher.util.EmulatorLauncher
import com.glyph.launcher.util.FolderMapping

/**
 * Setup flow: add folders per console -> scan -> scrape -> done.
 */
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    androidx.activity.compose.BackHandler(onBack = onBack)

    // SAF folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            val doc = DocumentFile.fromTreeUri(context, uri)
            val folderName = doc?.name ?: "Unknown Folder"
            viewModel.onFolderPicked(uri, folderName)
        }
    }

    val windowSize = rememberWindowSizeClass()
    val isCompact = windowSize == WindowSizeClass.COMPACT
    val topContentPadding = if (isCompact) 56.dp else 80.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlyphBlack)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(if (isCompact) 12.dp else 24.dp)
                .background(GlyphWhiteDisabled, RoundedCornerShape(4.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "BACK",
                style = PlatformLabel.copy(color = GlyphWhiteMedium)
            )
        }
        AnimatedContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topContentPadding),
            targetState = state.step,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "setup_step",
            contentAlignment = Alignment.Center
        ) { step ->
            when (step) {
                SetupViewModel.Step.WELCOME -> {
                    WelcomeStep(onNext = { viewModel.nextFromWelcome() })
                }

                SetupViewModel.Step.FOLDER_MANAGEMENT -> {
                    FolderManagementStep(
                        mappings = state.folderMappings,
                        error = state.error,
                        onAddFolder = { folderPickerLauncher.launch(null) },
                        onRemoveFolder = { viewModel.removeFolder(it) },
                        onNext = { viewModel.nextFromFolders() },
                        onStartScan = { viewModel.startScanning() }
                    )
                }

                SetupViewModel.Step.EMULATORS -> {
                    EmulatorsStep(
                        emulatorSteps = state.emulatorSteps,
                        onSetDefault = { viewModel.setEmulatorPreference(it.first, it.second) },
                        onClear = { viewModel.clearEmulatorPreference(it) },
                        onNext = { viewModel.nextFromEmulators() },
                        onDownload = { info -> EmulatorLauncher.openDownloadPage(context, info) }
                    )
                }

                SetupViewModel.Step.SCRAPER_API -> {
                    ScraperApiStep(
                        defaultScraper = state.defaultScraper,
                        theGamesDbApiKey = state.theGamesDbApiKey,
                        rawgApiKey = state.rawgApiKey,
                        mobyGamesApiKey = state.mobyGamesApiKey,
                        onDefaultScraperChange = { viewModel.setDefaultScraper(it) },
                        onTheGamesDbChange = { viewModel.updateTheGamesDbApiKey(it) },
                        onSaveTheGamesDb = { viewModel.saveTheGamesDbApiKey() },
                        onRawgChange = { viewModel.updateRawgApiKey(it) },
                        onSaveRawg = { viewModel.saveRawgApiKey() },
                        onMobyGamesChange = { viewModel.updateMobyGamesApiKey(it) },
                        onSaveMobyGames = { viewModel.saveMobyGamesApiKey() },
                        onNext = { viewModel.nextFromScraperApi() }
                    )
                }

                SetupViewModel.Step.SCANNING -> {
                    ScanningStep(progress = state.scanProgress)
                }

                SetupViewModel.Step.SCAN_COMPLETE -> {
                    ScanCompleteStep(
                        discoveredCount = state.totalDiscovered,
                        onStartScraping = { viewModel.startScraping() },
                        onSkip = { viewModel.skipScraping() }
                    )
                }

                SetupViewModel.Step.SCRAPING -> {
                    ScrapingStep(progress = state.scrapeProgress)
                }

                SetupViewModel.Step.DONE -> {
                    DoneStep(onContinue = onSetupComplete)
                }
            }
        }
    }

    // ── Platform Picker Dialog ───────────────────────────────────────────────
    if (state.showPlatformPicker) {
        PlatformPickerDialog(
            folderName = state.pendingFolderName ?: "",
            onPlatformSelected = { viewModel.onPlatformChosen(it.tag) },
            onDismiss = { viewModel.dismissPlatformPicker() }
        )
    }
}

// ── Step 0: Welcome ──────────────────────────────────────────────────────────

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Image(
            painter = painterResource(id = R.mipmap.ic_launcher_foreground),
            contentDescription = "RomVeil Logo",
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "RomVeil",
            style = GameTitleSelected.copy(letterSpacing = GlyphTypography.labelLarge.letterSpacing)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "RETRO GAME LAUNCHER",
            style = PlatformLabel.copy(color = GlyphDimmed)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "We’ll set up your library in a few steps:\n\n1. Pick ROM folders (by console)\n2. Choose default emulators\n3. Add scraper API keys (for covers)\n4. Scan & scrape",
            style = GameMetadata.copy(color = GlyphWhiteLow),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        GlyphButton(text = "GET STARTED", onClick = onNext)
    }
}

// ── Step 1: Folder Management ────────────────────────────────────────────────

@Composable
private fun FolderManagementStep(
    mappings: List<FolderMapping>,
    error: String?,
    onAddFolder: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onNext: () -> Unit,
    onStartScan: () -> Unit
) {
    val windowSize = rememberWindowSizeClass()
    val isCompact = windowSize == WindowSizeClass.COMPACT

    if (isCompact) {
        // ── Phone: vertical stacked layout ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "ROM FOLDERS",
                style = GameTitleSelected.copy(
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    letterSpacing = GlyphTypography.labelLarge.letterSpacing
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Add folders by console",
                style = PlatformLabel.copy(color = GlyphDimmed)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Pick a folder, then choose which console it belongs to.",
                style = GameMetadata.copy(color = GlyphWhiteLow)
            )
            Spacer(modifier = Modifier.height(16.dp))

            GlyphButton(text = "+ ADD FOLDER", onClick = onAddFolder)

            if (mappings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                GlyphButton(text = "NEXT → EMULATORS", onClick = onNext)
                Spacer(modifier = Modifier.height(6.dp))
                GlyphButtonSecondary(text = "SCAN ALL (skip emulators & scraper)", onClick = onStartScan)
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = error, style = GameMetadata.copy(color = PlatformNES))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (mappings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO FOLDERS ADDED YET",
                        style = GameMetadata.copy(color = GlyphWhiteLow)
                    )
                }
            } else {
                mappings.forEach { mapping ->
                    FolderMappingRow(
                        mapping = mapping,
                        onRemove = { onRemoveFolder(mapping.uri) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    } else {
        // ── Tablet / Landscape: horizontal Row layout ──
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .padding(end = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ROM FOLDERS",
                    style = GameTitleSelected.copy(letterSpacing = GlyphTypography.labelLarge.letterSpacing)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add folders by console",
                    style = PlatformLabel.copy(color = GlyphDimmed)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pick a folder, then choose which console it belongs to. This ensures the right emulator is used.",
                    style = GameMetadata.copy(color = GlyphWhiteLow)
                )
                Spacer(modifier = Modifier.height(24.dp))

                GlyphButton(text = "+ ADD FOLDER", onClick = onAddFolder)

                if (mappings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlyphButton(text = "NEXT → EMULATORS", onClick = onNext)
                    Spacer(modifier = Modifier.height(8.dp))
                    GlyphButtonSecondary(text = "SCAN ALL (skip emulators & scraper)", onClick = onStartScan)
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = error, style = GameMetadata.copy(color = PlatformNES))
                }
            }

            if (mappings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO FOLDERS ADDED YET",
                        style = GameMetadata.copy(color = GlyphWhiteLow)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mappings, key = { it.uri }) { mapping ->
                        FolderMappingRow(
                            mapping = mapping,
                            onRemove = { onRemoveFolder(mapping.uri) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderMappingRow(
    mapping: FolderMapping,
    onRemove: () -> Unit
) {
    val platform = Platform.fromTag(mapping.platformTag)
    val platformName = platform?.displayName ?: mapping.platformTag.uppercase()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlyphSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mapping.folderName,
                style = GlyphTypography.titleSmall.copy(color = GlyphWhiteHigh),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = platformName,
                style = GameMetadata.copy(color = GlyphDimmed)
            )
        }
        Text(
            text = "REMOVE",
            style = PlatformLabel.copy(color = PlatformNES),
            modifier = Modifier
                .clickable(onClick = onRemove)
                .padding(start = 12.dp)
        )
    }
}

// ── Step 2: Emulators ───────────────────────────────────────────────────────

@Composable
private fun EmulatorsStep(
    emulatorSteps: List<SetupViewModel.EmulatorStepState>,
    onSetDefault: (Pair<String, String>) -> Unit,
    onClear: (String) -> Unit,
    onNext: () -> Unit,
    onDownload: (EmulatorInfo) -> Unit
) {
    val isCompact = rememberWindowSizeClass() == WindowSizeClass.COMPACT
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isCompact) 16.dp else 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "DEFAULT EMULATORS",
            style = GameTitleSelected.copy(letterSpacing = GlyphTypography.labelLarge.letterSpacing)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose which emulator to use for each console. You can change this later in Settings.",
            style = GameMetadata.copy(color = GlyphWhiteLow)
        )
        Spacer(modifier = Modifier.height(24.dp))

        emulatorSteps.forEach { step ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .background(GlyphSurface, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = step.platform.displayName,
                    style = GlyphTypography.titleSmall.copy(color = GlyphWhiteHigh)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step.installed.isEmpty()) {
                        Text(
                            text = "No emulator installed",
                            style = GameMetadata.copy(color = GlyphDimmed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        GlyphButtonSecondary(text = "GET", onClick = {
                            step.platform.defaultEmulators.firstOrNull()?.let { onDownload(it) }
                        })
                    } else {
                        step.installed.forEach { emu ->
                            val selected = emu.packageName == step.preferredPackage
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (selected) GlyphWhiteHigh else GlyphWhiteDisabled,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onSetDefault(step.platform.tag to emu.packageName) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = emu.displayName,
                                    style = PlatformLabel.copy(color = if (selected) GlyphBlack else GlyphWhiteMedium)
                                )
                            }
                        }
                        if (step.preferredPackage != null) {
                            GlyphButtonSecondary(text = "RESET", onClick = { onClear(step.platform.tag) })
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        GlyphButton(text = "NEXT → SCRAPER API", onClick = onNext)
        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ── Step 3: Scraper API ──────────────────────────────────────────────────────

@Composable
private fun ScraperApiStep(
    defaultScraper: String,
    theGamesDbApiKey: String,
    rawgApiKey: String,
    mobyGamesApiKey: String,
    onDefaultScraperChange: (String) -> Unit,
    onTheGamesDbChange: (String) -> Unit,
    onSaveTheGamesDb: () -> Unit,
    onRawgChange: (String) -> Unit,
    onSaveRawg: () -> Unit,
    onMobyGamesChange: (String) -> Unit,
    onSaveMobyGames: () -> Unit,
    onNext: () -> Unit
) {
    val isCompact = rememberWindowSizeClass() == WindowSizeClass.COMPACT
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isCompact) 16.dp else 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "SCRAPER API",
            style = GameTitleSelected.copy(letterSpacing = GlyphTypography.labelLarge.letterSpacing)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Optional: add API keys to fetch game art and metadata. Get free keys at the links below.",
            style = GameMetadata.copy(color = GlyphWhiteLow)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Default scraper (try first)",
            style = GameMetadata.copy(color = GlyphWhiteLow)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "thegamesdb" to "TheGamesDB",
                "rawg" to "RAWG",
                "mobygames" to "MobyGames",
                "default" to "Default"
            ).forEach { (id, label) ->
                val selected = defaultScraper == id
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) GlyphWhiteHigh else GlyphWhiteDisabled,
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { onDefaultScraperChange(id) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = label,
                        style = PlatformLabel.copy(color = if (selected) GlyphBlack else GlyphWhiteMedium)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        SetupScraperField("TheGamesDB (api.thegamesdb.net)", theGamesDbApiKey, onTheGamesDbChange, onSaveTheGamesDb)
        Spacer(modifier = Modifier.height(12.dp))
        SetupScraperField("RAWG (rawg.io/apidocs)", rawgApiKey, onRawgChange, onSaveRawg)
        Spacer(modifier = Modifier.height(12.dp))
        SetupScraperField("MobyGames (mobygames.com/info/api)", mobyGamesApiKey, onMobyGamesChange, onSaveMobyGames)

        Spacer(modifier = Modifier.height(32.dp))
        GlyphButton(text = "SCAN & SCRAPE", onClick = onNext)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Scans your folders and optionally fetches covers/metadata using the APIs above.",
            style = GameMetadata.copy(color = GlyphDimmed)
        )
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun SetupScraperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlyphSurface, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = PlatformLabel.copy(color = GlyphDimmed)
        )
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = GlyphTypography.bodyMedium.copy(color = GlyphWhiteHigh),
            singleLine = true,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text("Enter API key (optional)", style = GlyphTypography.bodyMedium.copy(color = GlyphWhiteLow))
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        GlyphButtonSecondary(text = "SAVE", onClick = onSave)
    }
}

// ── Platform Picker Dialog ───────────────────────────────────────────────────

@Composable
private fun PlatformPickerDialog(
    folderName: String,
    onPlatformSelected: (Platform) -> Unit,
    onDismiss: () -> Unit
) {
    val windowSize = rememberWindowSizeClass()
    val isCompact = windowSize == WindowSizeClass.COMPACT
    val dialogWidth = if (isCompact) 0.95f else 0.6f
    val gridColumns = if (isCompact) 2 else 3

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(dialogWidth)
                .background(GlyphDarkSurface, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "CHOOSE CONSOLE",
                style = PlatformLabel.copy(color = GlyphWhiteMedium)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = folderName,
                style = GameMetadata.copy(color = GlyphDimmed)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Platform.entries.toList()) { platform ->
                    var isFocused by remember { mutableStateOf(false) }
                    
                    Box(
                        modifier = Modifier
                            .height(100.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .onFocusChanged { isFocused = it.isFocused }
                            .border(
                                width = if (isFocused) 2.dp else 0.dp,
                                color = if (isFocused) GlyphWhiteHigh else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                color = if (isFocused) GlyphSurface.copy(alpha = 0.8f) else GlyphSurface,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onPlatformSelected(platform) }
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = platform.tag.uppercase(),
                                style = GlyphTypography.titleSmall.copy(
                                    color = if (isFocused) GlyphWhiteHigh else GlyphWhiteMedium
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = platform.displayName,
                                style = GameMetadata.copy(
                                    color = if (isFocused) GlyphWhiteMedium else GlyphDimmed
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Remaining Steps (unchanged) ──────────────────────────────────────────────

@Composable
private fun ScanningStep(progress: SetupViewModel.ScanProgress?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Text(
            text = "SCANNING",
            style = PlatformLabel.copy(color = GlyphWhiteMedium)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(if (rememberWindowSizeClass() == WindowSizeClass.COMPACT) 0.8f else 0.4f),
            color = GlyphWhiteHigh,
            trackColor = GlyphWhiteDisabled
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (progress != null) {
            if (progress.currentFolder.isNotEmpty()) {
                Text(
                    text = progress.currentFolder,
                    style = GameMetadata.copy(color = GlyphWhiteMedium),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = "${progress.scannedCount} games found",
                style = GameMetadata.copy(color = GlyphDimmed)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = progress.currentFile,
                style = GameMetadata.copy(color = GlyphWhiteLow),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ScanCompleteStep(
    discoveredCount: Int,
    onStartScraping: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Text(
            text = "$discoveredCount",
            style = GameTitleSelected
        )
        Text(
            text = "GAMES FOUND",
            style = PlatformLabel.copy(color = GlyphDimmed)
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlyphButton(text = "SCRAPE METADATA", onClick = onStartScraping)
            GlyphButtonSecondary(text = "SKIP", onClick = onSkip)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Scraping downloads game info and artwork from TheGamesDB, RAWG, or MobyGames.",
            style = GameMetadata.copy(color = GlyphWhiteLow),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(if (rememberWindowSizeClass() == WindowSizeClass.COMPACT) 0.9f else 0.5f)
        )
    }
}

@Composable
private fun ScrapingStep(progress: SetupViewModel.ScrapeProgress?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Text(
            text = "SCRAPING",
            style = PlatformLabel.copy(color = GlyphWhiteMedium)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress.current.toFloat() / progress.total.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth(if (rememberWindowSizeClass() == WindowSizeClass.COMPACT) 0.8f else 0.4f),
                color = GlyphWhiteHigh,
                trackColor = GlyphWhiteDisabled,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${progress.current} / ${progress.total}",
                style = GameMetadata.copy(color = GlyphDimmed)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = progress.currentTitle,
                style = GameMetadata.copy(color = GlyphWhiteLow),
                maxLines = 1
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(if (rememberWindowSizeClass() == WindowSizeClass.COMPACT) 0.8f else 0.4f),
                color = GlyphWhiteHigh,
                trackColor = GlyphWhiteDisabled
            )
        }
    }
}

@Composable
private fun DoneStep(onContinue: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Text(
            text = "READY",
            style = GameTitleSelected
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "YOUR LIBRARY IS SET UP",
            style = PlatformLabel.copy(color = GlyphDimmed)
        )
        Spacer(modifier = Modifier.height(32.dp))
        GlyphButton(text = "START", onClick = onContinue)
    }
}

// ── Styled Buttons ───────────────────────────────────────────────────────────

@Composable
private fun GlyphButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(GlyphWhiteHigh, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = PlatformLabel.copy(color = GlyphBlack))
    }
}

@Composable
private fun GlyphButtonSecondary(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(GlyphWhiteDisabled, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = PlatformLabel.copy(color = GlyphWhiteMedium))
    }
}
