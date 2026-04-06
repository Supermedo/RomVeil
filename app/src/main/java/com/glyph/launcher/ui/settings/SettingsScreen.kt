package com.glyph.launcher.ui.settings

import android.content.Intent
import android.net.Uri
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glyph.launcher.MainActivity
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.glyph.launcher.domain.model.EmulatorInfo
import com.glyph.launcher.domain.model.Platform
import com.glyph.launcher.util.FolderMapping
import com.glyph.launcher.ui.theme.*
import com.glyph.launcher.util.EmulatorLauncher

private enum class SettingsPage {
    MAIN,
    LIBRARY,
    MANAGE_SYSTEMS,
    SCRAPER,
    ABOUT,
    PRIVACY_POLICY
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRescan: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var currentPage by remember { mutableStateOf(SettingsPage.MAIN) }

    // Handle Back Button
    BackHandler {
        if (currentPage == SettingsPage.MAIN) {
            onBack()
        } else {
            currentPage = SettingsPage.MAIN
        }
    }

    val activity = context as? MainActivity
    DisposableEffect(activity) {
        activity?.setKeyEventListener { event ->
            if (event.action == AndroidKeyEvent.ACTION_DOWN &&
                (event.keyCode == AndroidKeyEvent.KEYCODE_BACK || event.keyCode == AndroidKeyEvent.KEYCODE_BUTTON_B)
            ) {
                if (currentPage == SettingsPage.MAIN) {
                    onBack()
                } else {
                    currentPage = SettingsPage.MAIN
                }
                true
            } else false
        }
        onDispose { }
    }

    // Launchers
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            val doc = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
            val folderName = doc?.name ?: "Unknown Folder"
            viewModel.onFolderPicked(uri, folderName)
        }
    }

    val romFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onRomFilePicked(it) }
    }

    // UI Structure
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlyphBlack)
            .padding(32.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header: MENU (only on Main Page, subpages have their own headers)
            if (currentPage == SettingsPage.MAIN) {
                 Text(
                    text = "MENU",
                    style = GlyphTypography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            } else {
                // Subpage Header with Back button visual
                 Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "BACK",
                        style = GlyphTypography.titleMedium.copy(color = GlyphWhiteMedium, fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .clickable { currentPage = SettingsPage.MAIN }
                            .padding(end = 16.dp)
                    )
                    Text(
                        text = when(currentPage) {
                            SettingsPage.LIBRARY -> "LIBRARY"
                            SettingsPage.MANAGE_SYSTEMS -> "MANAGE SYSTEMS"
                            SettingsPage.SCRAPER -> "SCRAPER"
                            SettingsPage.ABOUT -> "ABOUT"
                            SettingsPage.PRIVACY_POLICY -> "PRIVACY POLICY"
                            else -> ""
                        },
                        style = GlyphTypography.displayMedium.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }

            // Content Switcher
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState == SettingsPage.MAIN) {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    } else {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    }
                },
                label = "SettingsPageTransition"
            ) { page ->
                when (page) {
                    SettingsPage.MAIN -> SettingsMainMenu(
                        onNavigate = { currentPage = it },
                        onBack = onBack
                    )
                    SettingsPage.LIBRARY -> LibraryPage(
                        state = state,
                        viewModel = viewModel,
                        onAddFolder = { folderPickerLauncher.launch(null) },
                        onAddFile = { romFilePickerLauncher.launch("*/*") },
                        onRescan = onRescan
                    )
                    SettingsPage.MANAGE_SYSTEMS -> ManageSystemsPage(
                        state = state,
                        viewModel = viewModel
                    )
                    SettingsPage.SCRAPER -> ScraperPage(
                        state = state,
                        viewModel = viewModel
                    )
                    SettingsPage.ABOUT -> AboutPage(
                        state = state,
                        onPrivacyPolicy = { currentPage = SettingsPage.PRIVACY_POLICY }
                    )
                    SettingsPage.PRIVACY_POLICY -> PrivacyPolicyPage()
                }
            }
        }
    }

    // ── Platform Pickers & Dialogs ──
    if (state.showPlatformPicker) {
        SettingsPlatformPickerDialog(
            label = state.pendingFolderName ?: "",
            onPlatformSelected = { viewModel.onPlatformChosenForFolder(it.tag) },
            onDismiss = { viewModel.dismissPlatformPicker() }
        )
    }

    if (state.showRomPlatformPicker) {
        SettingsPlatformPickerDialog(
            label = state.pendingRomFilename ?: "",
            onPlatformSelected = { viewModel.onPlatformChosenForRom(it.tag) },
            onDismiss = { viewModel.dismissRomPlatformPicker() }
        )
    }

    if (state.showScrapePrompt) {
        val platformName = state.lastAddedPlatform?.let { Platform.fromTag(it)?.displayName } ?: "New"
        AlertDialog(
            onDismissRequest = { viewModel.dismissScrapePrompt() },
            title = {
                Text(
                    text = "Scrape $platformName games?",
                    style = GlyphTypography.titleLarge.copy(color = GlyphWhiteHigh)
                )
            },
            text = {
                Text(
                    text = "Found new games. Would you like to download metadata and box art now?",
                    style = GlyphTypography.bodyMedium.copy(color = GlyphWhiteMedium)
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.startScrapeFromPrompt() }) {
                    Text("SCRAPE NOW", style = PlatformLabel.copy(color = GlyphAccent))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissScrapePrompt() }) {
                    Text("SKIP", style = PlatformLabel.copy(color = GlyphWhiteMedium))
                }
            },
            containerColor = GlyphDarkSurface
        )
    }
}

// ── Main Menu ────────────────────────────────────────────────────────────────

@Composable
private fun SettingsMainMenu(
    onNavigate: (SettingsPage) -> Unit,
    onBack: () -> Unit
) {
    // Define menu items with their actions
    val menuItems = listOf<Pair<String, () -> Unit>>(
        "LIBRARY" to { onNavigate(SettingsPage.LIBRARY) },
        "MANAGE SYSTEMS" to { onNavigate(SettingsPage.MANAGE_SYSTEMS) },
        "SCRAPER" to { onNavigate(SettingsPage.SCRAPER) },
        "ABOUT" to { onNavigate(SettingsPage.ABOUT) },
        "BACK" to onBack
    )

    // Focus handling
    val focusRequesters = remember { List(menuItems.size) { FocusRequester() } }
    
    LaunchedEffect(Unit) {
        // Request focus on the first item (LIBRARY)
        kotlinx.coroutines.delay(100)
        focusRequesters[0].requestFocus() 
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(menuItems.size) { index ->
            val (label, action) = menuItems[index]
            MenuButton(
                text = label,
                focusRequester = focusRequesters[index],
                onClick = action
            )
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    focusRequester: FocusRequester,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    
    Text(
        text = text,
        style = GlyphTypography.headlineLarge.copy(
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = if (focused) Color.White else Color.Gray.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.hasFocus }
            .clickable { onClick() }
            .focusable()
            .padding(vertical = 4.dp)
    )
}

// ── Sub Pages ────────────────────────────────────────────────────────────────

@Composable
private fun LibraryPage(
    state: SettingsViewModel.UiState,
    viewModel: SettingsViewModel,
    onAddFolder: () -> Unit,
    onAddFile: () -> Unit,
    onRescan: () -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }

     Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
         SettingsItem(
             title = "Add folder",
             subtitle = "Pick a folder, assign to a console",
             action = "ADD",
             onClick = onAddFolder
         )
         Spacer(modifier = Modifier.height(16.dp))
         SettingsItem(
             title = "Add ROM file",
             subtitle = "Pick a single ROM file manually",
             action = "ADD",
             onClick = onAddFile
         )
         Spacer(modifier = Modifier.height(16.dp))
         SettingsItem(
             title = "Rescan Library",
             subtitle = "Scan added folders for new games",
             action = if (state.isRescanning) "SCANNING..." else "SCAN",
             onClick = {
                 if (!state.isRescanning) {
                     viewModel.rescan()
                     onRescan()
                 }
             }
         )
         Spacer(modifier = Modifier.height(16.dp))
         SettingsItem(
             title = "Clear Library",
             subtitle = "Remove all games and cached data",
             action = "CLEAR",
             isDangerous = true,
             onClick = { showClearConfirm = true }
         )
         
         if (state.folderMappings.isNotEmpty()) {
             Spacer(modifier = Modifier.height(32.dp))
             Text("MAPPED FOLDERS", style = GlyphTypography.titleMedium.copy(color = GlyphDimmed))
             Spacer(modifier = Modifier.height(8.dp))
             
             state.folderMappings.forEach { mapping ->
                 val platform = Platform.fromTag(mapping.platformTag)
                 val platformName = platform?.displayName ?: mapping.platformTag.uppercase()
                 
                 SettingsItem(
                     title = mapping.folderName,
                     subtitle = "$platformName (${mapping.uri})",
                     action = "REMOVE",
                     isDangerous = true,
                     onClick = { viewModel.removeFolder(mapping.uri) }
                 )
                 Spacer(modifier = Modifier.height(8.dp))
             }
         }
     }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Library?", color = Color.White) },
            text = { Text("Delete all games and metadata? ROMs will stay.", color = Color.Gray) },
            confirmButton = {
                TextButton(onClick = { 
                    showClearConfirm = false
                    viewModel.clearLibrary() 
                }) { Text("CLEAR", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("CANCEL", color = Color.White) }
            },
            containerColor = GlyphDarkSurface
        )
    }
}

@Composable
private fun ManageSystemsPage(
    state: SettingsViewModel.UiState,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(state.platformEmulators.entries.toList()) { (tag, emulatorState) ->
            val platform = Platform.fromTag(tag) ?: return@items
            val detection = EmulatorLauncher.detectInstalledEmulators(context, platform)
            
            EmulatorSettingsRow(
                platform = platform,
                installed = detection.installed,
                notInstalled = detection.notInstalled,
                preferredPackage = emulatorState.preferredPackage,
                onSetDefault = { pkg -> viewModel.setEmulatorPreference(tag, pkg) },
                onClearPreference = { viewModel.clearEmulatorPreference(tag) },
                onDownloadEmulator = { emu ->
                    if (EmulatorLauncher.isEmulatorInstalled(context, emu.packageName)) {
                        viewModel.setEmulatorPreference(tag, emu.packageName)
                    } else {
                        EmulatorLauncher.openDownloadPage(context, emu)
                    }
                }
            )
        }
    }
}

@Composable
private fun ScraperPage(
    state: SettingsViewModel.UiState,
    viewModel: SettingsViewModel
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (state.isScraping) {
             ScrapingProgress(state, viewModel)
             Spacer(modifier = Modifier.height(24.dp))
        }

        SettingsItem(
            title = "Scrape missing",
            subtitle = "Only games not yet scraped (PENDING)",
            action = if (state.isScraping) "..." else "SCRAPE",
            onClick = { if (!state.isScraping) viewModel.rescrapeAll() }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            title = "Re-scrape all",
            subtitle = "Reset all to PENDING then scrape",
            action = if (state.isScraping) "..." else "RESET & SCRAPE",
            onClick = { if (!state.isScraping) viewModel.resetAndRescrapeAll() }
        )

        Spacer(modifier = Modifier.height(32.dp))
        Text("DEFAULT SOURCE", style = GlyphTypography.titleMedium.copy(color = GlyphDimmed))
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            listOf(
                "screenscraper" to "ScreenScraper.fr",
                "thegamesdb" to "TheGamesDB",
                "rawg" to "RAWG",
                "mobygames" to "MobyGames",
                "default" to "Default"
            ).forEach { (id, label) ->
                SettingsChip(
                    label = label,
                    isSelected = state.defaultScraper == id,
                    onClick = { viewModel.setDefaultScraper(id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("API KEYS", style = GlyphTypography.titleMedium.copy(color = GlyphDimmed))
        Spacer(modifier = Modifier.height(16.dp))

        // ScreenScraper
        Text("ScreenScraper.fr", style = GlyphTypography.titleSmall.copy(color = GlyphWhiteHigh))
        Spacer(modifier = Modifier.height(8.dp))
        ScraperField("User ID", state.ssUser, { viewModel.updateSsUser(it) })
        Spacer(modifier = Modifier.height(4.dp))
        ScraperField("User Pass", state.ssPass, { viewModel.updateSsPass(it) }, true)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsChip("SAVE CREDENTIALS", true, { viewModel.saveSsCredentials() })
        
        Spacer(modifier = Modifier.height(24.dp))

        // TheGamesDB
        Text("TheGamesDB", style = GlyphTypography.titleSmall.copy(color = GlyphWhiteHigh))
        Spacer(modifier = Modifier.height(8.dp))
        ScraperField("API Key", state.theGamesDbApiKey, { viewModel.updateTheGamesDbApiKey(it) })
        Spacer(modifier = Modifier.height(8.dp))
        SettingsChip("SAVE", true, { viewModel.saveTheGamesDbApiKey() })

        Spacer(modifier = Modifier.height(24.dp))
        
        // MobyGames
        Text("MobyGames", style = GlyphTypography.titleSmall.copy(color = GlyphWhiteHigh))
        Spacer(modifier = Modifier.height(8.dp))
        ScraperField("API Key", state.mobyGamesApiKey, { viewModel.updateMobyGamesApiKey(it) })
        Spacer(modifier = Modifier.height(8.dp))
        SettingsChip("SAVE", true, { viewModel.saveMobyGamesApiKey() })

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun ScrapingProgress(state: SettingsViewModel.UiState, viewModel: SettingsViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlyphSurface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        val p = state.scrapeProgress
        if (p != null && p.total > 0) {
            val percent = ((p.current.toFloat() / p.total) * 100).toInt()
            Text("Scraping: ${p.current} / ${p.total} ($percent%)", style = GlyphTypography.titleSmall.copy(color = GlyphWhiteHigh))
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { p.current.toFloat() / p.total },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Text(p.currentTitle, style = GlyphTypography.bodySmall.copy(color = GlyphDimmed))
        } else {
             LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp))
             Text("Preparing...", style = GlyphTypography.bodySmall.copy(color = GlyphDimmed))
        }
        Spacer(modifier = Modifier.height(8.dp))
        SettingsChip("STOP", false, { viewModel.stopScraping() })
    }
}



@Composable
private fun AboutPage(
    state: SettingsViewModel.UiState,
    onPrivacyPolicy: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── App Icon + Name ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            val iconPainter = androidx.compose.ui.res.painterResource(
                id = com.glyph.launcher.R.drawable.ic_romveil_logo
            )
            androidx.compose.foundation.Image(
                painter = iconPainter,
                contentDescription = "RomVeil Logo",
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2A1F3D),
                                Color(0xFF1A1425)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    "RomVeil",
                    style = GlyphTypography.headlineLarge.copy(
                        color = GlyphWhiteHigh,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "v1.0.0",
                    style = GlyphTypography.titleMedium.copy(color = GlyphAccent)
                )
            }
        }

        // ── Description ──
        Text(
            "A typography-centric retro game launcher designed for simplicity and style.",
            style = GlyphTypography.bodyLarge.copy(
                color = GlyphWhiteMedium,
                lineHeight = 24.sp
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Creator ──
        Text(
            "DEVELOPER",
            style = GlyphTypography.labelLarge.copy(
                color = GlyphAccent,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Mohammed Albarghouthi",
            style = GlyphTypography.bodyLarge.copy(color = GlyphWhiteHigh)
        )
        Text(
            "buymeacoffee.com/mohmmadpodt",
            style = GlyphTypography.bodyMedium.copy(color = GlyphAccent),
            modifier = Modifier.clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/mohmmadpodt"))
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Tech ──
        Text(
            "BUILT WITH",
            style = GlyphTypography.labelLarge.copy(
                color = GlyphAccent,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Jetpack Compose • Kotlin • Room • Hilt • Retrofit",
            style = GlyphTypography.bodyMedium.copy(color = GlyphWhiteMedium)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Credits ──
        Text(
            "METADATA CREDITS",
            style = GlyphTypography.labelLarge.copy(
                color = GlyphAccent,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "ScreenScraper • TheGamesDB • RAWG • MobyGames • LibRetro",
            style = GlyphTypography.bodyMedium.copy(color = GlyphWhiteMedium)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Privacy Policy Link ──
        var ppFocused by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (ppFocused) GlyphWhite else GlyphSurface,
                    RoundedCornerShape(8.dp)
                )
                .clickable(onClick = onPrivacyPolicy)
                .focusable()
                .onFocusChanged { ppFocused = it.hasFocus }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Privacy Policy",
                    style = GlyphTypography.titleMedium.copy(
                        color = if (ppFocused) GlyphBlack else GlyphWhiteHigh,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Read our privacy policy and data practices",
                    style = GlyphTypography.bodySmall.copy(
                        color = if (ppFocused) GlyphBlack.copy(alpha = 0.7f) else GlyphDimmed
                    )
                )
            }
            Text(
                text = "VIEW",
                style = GlyphTypography.labelLarge.copy(
                    color = if (ppFocused) GlyphBlack else GlyphAccent,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Copyright ──
        Text(
            "© 2025 Mohammed Albarghouthi. All rights reserved.",
            style = GlyphTypography.bodySmall.copy(color = GlyphDimmed),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Privacy Policy Page ──────────────────────────────────────────────────────

@Composable
private fun PrivacyPolicyPage() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            "Last updated: February 15, 2025",
            style = GlyphTypography.bodySmall.copy(color = GlyphDimmed)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ── Intro ──
        PolicySection(
            title = "Introduction",
            body = "RomVeil (\"the App\") is a retro game launcher for Android developed by " +
                    "Mohammed Albarghouthi. This Privacy Policy explains how the App collects, " +
                    "uses, and protects your information. By installing or using RomVeil, you " +
                    "agree to the practices described in this Privacy Policy."
        )

        // ── Data Collection ──
        PolicySection(
            title = "Information We Collect",
            body = "RomVeil is designed with privacy in mind. The App does NOT collect, " +
                    "transmit, or store any personal information on external servers.\n\n" +
                    "• Local Data Only: All data (game library, settings, preferences, cover " +
                    "art, metadata) is stored exclusively on your device.\n\n" +
                    "• No Accounts: RomVeil does not require user registration or sign-in.\n\n" +
                    "• No Analytics: We do not use any analytics, tracking, or advertising SDKs.\n\n" +
                    "• No Crash Reporting: We do not collect crash logs or diagnostic data automatically."
        )

        // ── Permissions ──
        PolicySection(
            title = "Permissions",
            body = "RomVeil requests the following permissions, which are essential for its " +
                    "core functionality:\n\n" +
                    "• Storage Access (READ/WRITE_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE): " +
                    "Required to scan and read ROM files from your device storage so that " +
                    "emulators can load them. No files are uploaded, shared, or modified.\n\n" +
                    "• Internet Access (INTERNET, ACCESS_NETWORK_STATE): Used exclusively to " +
                    "download game metadata (titles, cover art, release dates, developer info) " +
                    "from the following third-party services:\n" +
                    "  – ScreenScraper (screenscraper.fr)\n" +
                    "  – TheGamesDB (thegamesdb.net)\n" +
                    "  – RAWG (rawg.io)\n" +
                    "  – MobyGames (mobygames.com)\n" +
                    "  – LibRetro Thumbnails (thumbnails.libretro.com)\n\n" +
                    "• Package Visibility (QUERY_ALL_PACKAGES via \u003cqueries\u003e): Used to detect " +
                    "which emulator apps are installed on your device so RomVeil can offer " +
                    "to launch games with the appropriate emulator."
        )

        // ── Third-Party Services ──
        PolicySection(
            title = "Third-Party Services",
            body = "When you use the scraping feature, RomVeil sends game file names (not " +
                    "file contents) to the metadata providers listed above to retrieve cover " +
                    "art and game information. These requests are standard HTTPS API calls. " +
                    "No personal data, device identifiers, or account information is included " +
                    "in these requests.\n\n" +
                    "Each third-party service has its own privacy policy:\n" +
                    "• ScreenScraper: screenscraper.fr\n" +
                    "• TheGamesDB: thegamesdb.net\n" +
                    "• RAWG: rawg.io/apidocs\n" +
                    "• MobyGames: mobygames.com"
        )

        // ── Data Retention ──
        PolicySection(
            title = "Data Storage & Retention",
            body = "All data is stored locally on your Android device using an encrypted " +
                    "Room database. This includes:\n\n" +
                    "• Your game library and metadata\n" +
                    "• Downloaded cover art images\n" +
                    "• Your emulator preferences\n" +
                    "• API keys you choose to provide for scraping services\n\n" +
                    "Uninstalling RomVeil will permanently delete all locally stored data. " +
                    "No data persists on any server after uninstallation."
        )

        // ── Children ──
        PolicySection(
            title = "Children's Privacy",
            body = "RomVeil is not directed at children under the age of 13. The App does " +
                    "not knowingly collect any personal information from children. Since " +
                    "RomVeil does not collect any personal data from any user, there is no " +
                    "risk of children's data being collected."
        )

        // ── Security ──
        PolicySection(
            title = "Data Security",
            body = "Since all data remains on your device and no data is transmitted to our " +
                    "servers, your information is protected by your device's own security " +
                    "measures (screen lock, encryption, etc.). We recommend keeping your " +
                    "device software up to date for the best security."
        )

        // ── User Rights ──
        PolicySection(
            title = "Your Rights",
            body = "You have full control over your data:\n\n" +
                    "• Access: All your data is visible within the App.\n" +
                    "• Deletion: You can delete individual games, clear your library, or " +
                    "uninstall the App to remove all data.\n" +
                    "• Portability: Your ROM files remain untouched on your device's storage.\n" +
                    "• Opt-out: You can use RomVeil without the scraping feature if you " +
                    "prefer not to make any network requests."
        )

        // ── ROM Files Disclaimer ──
        PolicySection(
            title = "ROM Files Disclaimer",
            body = "RomVeil is a game launcher — it does NOT include, distribute, or " +
                    "download ROM files. Users are solely responsible for ensuring they " +
                    "have the legal right to use any ROM files on their device. RomVeil " +
                    "only reads file names and metadata for display purposes; it does not " +
                    "modify, copy, or transmit ROM file contents."
        )

        // ── Changes ──
        PolicySection(
            title = "Changes to This Policy",
            body = "We may update this Privacy Policy from time to time. Any changes will " +
                    "be reflected in the \"Last updated\" date at the top of this page. " +
                    "Continued use of the App after changes constitutes acceptance of the " +
                    "updated policy."
        )

        // ── Contact ──
        PolicySection(
            title = "Contact Us",
            body = "If you have any questions about this Privacy Policy or the App's " +
                    "practices, please contact us at:\n\n" +
                    "Mohammed Albarghouthi\n" +
                    "Email: mohmmad.pod@gmail.com\n" +
                    "Website: buymeacoffee.com/mohmmadpodt"
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Text(
        text = title.uppercase(),
        style = GlyphTypography.labelLarge.copy(
            color = GlyphAccent,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = body,
        style = GlyphTypography.bodyMedium.copy(
            color = GlyphWhiteMedium,
            lineHeight = 22.sp
        )
    )
    Spacer(modifier = Modifier.height(20.dp))
}

// ── Shared Components ────────────────────────────────────────────────────────

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    action: String,
    isDangerous: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (focused) GlyphWhite else GlyphSurface,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .focusable()
            .onFocusChanged { focused = it.hasFocus }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GlyphTypography.titleMedium.copy(
                    color = if (focused) GlyphBlack else GlyphWhiteHigh,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = subtitle,
                style = GlyphTypography.bodySmall.copy(
                    color = if (focused) GlyphBlack.copy(alpha=0.7f) else GlyphDimmed
                )
            )
        }
        Text(
            text = action,
            style = GlyphTypography.labelLarge.copy(
                color = if (focused) GlyphBlack else if (isDangerous) PlatformNES else GlyphAccent,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun SettingsChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val active = isSelected || focused
    
    Box(
        modifier = Modifier
            .background(
                if (active) GlyphWhite else GlyphWhiteDisabled,
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .focusable()
            .onFocusChanged { focused = it.hasFocus }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = GlyphTypography.labelLarge.copy(
                color = if (active) GlyphBlack else GlyphWhiteMedium,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun ScraperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    
    Column {
        Text(label, style = GlyphTypography.labelMedium.copy(color = GlyphDimmed))
        Spacer(modifier = Modifier.height(4.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = GlyphTypography.bodyLarge.copy(color = if (focused) GlyphBlack else GlyphWhiteHigh),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (focused) GlyphWhite else GlyphSurface,
                    RoundedCornerShape(4.dp)
                )
                .focusable()
                .onFocusChanged { focused = it.hasFocus }
                .padding(12.dp)
        )
    }
}

@Composable
private fun EmulatorSettingsRow(
    platform: Platform,
    installed: List<EmulatorInfo>,
    notInstalled: List<EmulatorInfo>,
    preferredPackage: String?,
    onSetDefault: (packageName: String) -> Unit,
    onClearPreference: () -> Unit,
    onDownloadEmulator: (EmulatorInfo) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GlyphSurface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(platform.displayName, style = GlyphTypography.titleMedium.copy(color = GlyphWhiteHigh, fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            installed.forEach { emu ->
                SettingsChip(
                    label = emu.displayName + (if (emu.packageName == preferredPackage) " (Default)" else ""),
                    isSelected = emu.packageName == preferredPackage,
                    onClick = { onSetDefault(emu.packageName) }
                )
            }
            notInstalled.forEach { emu ->
                 SettingsChip(
                    label = "${emu.displayName} (Get)",
                    isSelected = false,
                    onClick = { onDownloadEmulator(emu) }
                )
            }
        }
    }
}

@Composable
private fun SettingsPlatformPickerDialog(
    label: String,
    onPlatformSelected: (Platform) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(GlyphDarkSurface, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text("SELECT PLATFORM", style = GlyphTypography.titleMedium.copy(color = GlyphWhiteHigh))
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Platform.entries) { platform ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(GlyphSurface, RoundedCornerShape(8.dp))
                            .clickable { onPlatformSelected(platform) }
                            .padding(12.dp)
                    ) {
                        Text(platform.tag.uppercase(), color = GlyphWhiteMedium)
                    }
                }
            }
        }
    }
}

