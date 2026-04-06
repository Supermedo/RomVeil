package com.glyph.launcher.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glyph.launcher.BuildConfig
import com.glyph.launcher.data.repository.GameRepository
import com.glyph.launcher.data.repository.ScrapingController
import com.glyph.launcher.domain.model.Platform
import com.glyph.launcher.util.EmulatorLauncher
import com.glyph.launcher.util.FolderMapping
import com.glyph.launcher.util.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: GameRepository,
    private val preferencesManager: PreferencesManager,
    private val scrapingController: ScrapingController
) : ViewModel() {

    data class EmulatorState(
        val preferred: String?,           // display name for UI
        val preferredPackage: String?,   // package name for setting default
        val installedCount: Int
    )

    data class ScrapeProgress(
        val current: Int,
        val total: Int,
        val currentTitle: String
    )

    data class UiState(
        val gameCount: Int = 0,
        val folderMappings: List<FolderMapping> = emptyList(),
        val romsFolderUri: String? = null,
        val platformEmulators: Map<String, EmulatorState> = emptyMap(),
        val isScraping: Boolean = false,
        val showPlatformPicker: Boolean = false,
        val pendingFolderName: String? = null,
        val pendingFolderUri: String? = null,
        val showRomPlatformPicker: Boolean = false,
        val pendingRomUri: String? = null,
        val pendingRomFilename: String? = null,
        val scrapeProgress: ScrapeProgress? = null,
        val theGamesDbApiKey: String = "",
        val theGamesDbSaved: Boolean = false,
        val rawgApiKey: String = "",
        val rawgSaved: Boolean = false,
        val mobyGamesApiKey: String = "",
        val mobyGamesSaved: Boolean = false,
        val defaultScraper: String = "thegamesdb",
        val twitchLabel: String = "Default",  // Shown in Settings; "Default" when Retrosm credentials are set (never show ID/secret)
        val isRescanning: Boolean = false,
        val scanProgressCount: Int = 0,
        val scanCurrentFile: String? = null,
        val ssDevId: String = "",
        val ssDevPass: String = "",
        val ssUser: String = "",
        val ssPass: String = "",
        val ssSaved: Boolean = false,
        val showScrapePrompt: Boolean = false,
        val lastAddedPlatform: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.folderMappings.collect { mappings ->
                _uiState.update { it.copy(folderMappings = mappings) }
            }
        }
        loadSettings()
        viewModelScope.launch {
            var wasScraping = false
            scrapingController.state.collect { s ->
                if (wasScraping && !s.isScraping) loadSettings()
                wasScraping = s.isScraping
                _uiState.update {
                    it.copy(
                        isScraping = s.isScraping,
                        scrapeProgress = s.progress?.let { p -> ScrapeProgress(p.current, p.total, p.currentTitle) }
                    )
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val gameCount = repository.getGameCount()
            val romsUri = preferencesManager.romsFolderUri.first()
            // Show emulator settings for ALL platforms (not only those with games) so user can set defaults and see download options
            val allPlatformTags = Platform.entries.map { it.tag }

            val emulatorMap = mutableMapOf<String, EmulatorState>()
            for (tag in allPlatformTags) {
                val platform = Platform.fromTag(tag) ?: continue
                val preferredPkg = preferencesManager.getEmulatorPreference(tag).first()?.takeIf { it.isNotBlank() }
                val detection = EmulatorLauncher.detectInstalledEmulators(context, platform)
                emulatorMap[tag] = EmulatorState(
                    preferred = preferredPkg?.let { pkg ->
                        platform.defaultEmulators.find { it.packageName == pkg }?.displayName ?: pkg
                    },
                    preferredPackage = preferredPkg,
                    installedCount = detection.installed.size
                )
            }

            val tgdbKey = preferencesManager.theGamesDbApiKey.first()
            val rawgKey = preferencesManager.rawgApiKey.first()
            val mobyKey = preferencesManager.mobyGamesApiKey.first()
            val defaultScraper = preferencesManager.defaultScraper.first()
            val twitchLabel = if (BuildConfig.TWITCH_CLIENT_ID.isNotBlank()) "Default" else "Not set"

            val ssDevId = preferencesManager.ssDevId.first() ?: ""
            val ssDevPass = preferencesManager.ssDevPass.first() ?: ""
            val ssUser = preferencesManager.ssUser.first() ?: ""
            val ssPass = preferencesManager.ssPass.first() ?: ""

            _uiState.update {
                it.copy(
                    gameCount = gameCount,
                    romsFolderUri = romsUri,
                    platformEmulators = emulatorMap,
                    theGamesDbApiKey = tgdbKey,
                    theGamesDbSaved = false,
                    rawgApiKey = rawgKey,
                    rawgSaved = false,
                    mobyGamesApiKey = mobyKey,
                    mobyGamesSaved = false,
                    defaultScraper = defaultScraper,
                    twitchLabel = twitchLabel,
                    ssDevId = ssDevId,
                    ssDevPass = ssDevPass,
                    ssUser = ssUser,
                    ssPass = ssPass,
                    ssSaved = false
                )
            }
        }
    }

    fun onFolderPicked(uri: Uri, folderName: String) {
        _uiState.update {
            it.copy(
                showPlatformPicker = true,
                pendingFolderName = folderName,
                pendingFolderUri = uri.toString()
            )
        }
    }

    fun onPlatformChosenForFolder(platformTag: String) {
        val uri = _uiState.value.pendingFolderUri ?: return
        val folderName = _uiState.value.pendingFolderName ?: return
        viewModelScope.launch {
            val mapping = FolderMapping(uri, platformTag, folderName)
            preferencesManager.addFolderMapping(mapping)
            val platform = Platform.fromTag(platformTag)
            if (platform != null) {
                _uiState.update { it.copy(isRescanning = true, scanProgressCount = 0, scanCurrentFile = null) }
                var totalFound = 0
                runCatching {
                    withContext(Dispatchers.IO) {
                        repository.scanDirectory(Uri.parse(uri), platform) { count, name ->
                            viewModelScope.launch(Dispatchers.Main.immediate) {
                                _uiState.update { it.copy(scanProgressCount = count, scanCurrentFile = name) }
                            }
                        }.let { totalFound = it.discovered.size }
                    }
                }
                if (totalFound > 0) {
                     _uiState.update { it.copy(isRescanning = false, scanCurrentFile = null, showScrapePrompt = true, lastAddedPlatform = platformTag) }
                } else {
                     _uiState.update { it.copy(isRescanning = false, scanCurrentFile = null) }
                }
            }
            _uiState.update {
                it.copy(showPlatformPicker = false, pendingFolderName = null, pendingFolderUri = null)
            }
            loadSettings()
        }
    }

    fun dismissPlatformPicker() {
        _uiState.update {
            it.copy(showPlatformPicker = false, pendingFolderName = null, pendingFolderUri = null)
        }
    }

    fun removeFolder(uri: String) {
        viewModelScope.launch {
            preferencesManager.removeFolderMapping(uri)
            loadSettings()
        }
    }

    fun onRomFilePicked(uri: Uri) {
        viewModelScope.launch {
            val filename = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } ?: uri.lastPathSegment?.substringAfterLast("/", "")
                }.getOrNull()
            } ?: return@launch
            val ext = "." + filename.substringAfterLast(".", "").lowercase()
            val platforms = Platform.fromExtension(ext)
            if (platforms.size == 1) {
                addSingleRom(uri, platforms.first().tag)
            } else if (platforms.isEmpty()) {
                _uiState.update { it.copy(pendingRomUri = null, pendingRomFilename = null) }
            } else {
                _uiState.update {
                    it.copy(
                        showRomPlatformPicker = true,
                        pendingRomUri = uri.toString(),
                        pendingRomFilename = filename
                    )
                }
            }
        }
    }

    fun onPlatformChosenForRom(platformTag: String) {
        val uriStr = _uiState.value.pendingRomUri ?: return
        _uiState.update {
            it.copy(showRomPlatformPicker = false, pendingRomUri = null, pendingRomFilename = null)
        }
        addSingleRom(Uri.parse(uriStr), platformTag)
    }

    fun dismissRomPlatformPicker() {
        _uiState.update {
            it.copy(showRomPlatformPicker = false, pendingRomUri = null, pendingRomFilename = null)
        }
    }

    private fun addSingleRom(uri: Uri, platformTag: String) {
        viewModelScope.launch {
            val game = repository.addSingleRom(uri, platformTag)
            if (game != null) loadSettings()
        }
    }

    /** Re-scan only the already added folder(s). Does not navigate to Setup or trigger rescrape. */
    fun rescan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRescanning = true, scanProgressCount = 0, scanCurrentFile = null) }
            runCatching {
                var totalFound = 0
                val mappings = preferencesManager.getFolderMappingsOnce()
                if (mappings.isNotEmpty()) {
                    for (mapping in mappings) {
                        val platform = Platform.fromTag(mapping.platformTag)
                        val result = withContext(Dispatchers.IO) {
                            repository.scanDirectory(Uri.parse(mapping.uri), platform) { count, name ->
                                viewModelScope.launch(Dispatchers.Main.immediate) {
                                    _uiState.update {
                                        it.copy(scanProgressCount = totalFound + count, scanCurrentFile = name)
                                    }
                                }
                            }
                        }
                        totalFound += result.discovered.size
                    }
                } else {
                    val uri = preferencesManager.romsFolderUri.first()
                    if (uri != null) {
                        withContext(Dispatchers.IO) {
                            repository.scanDirectory(Uri.parse(uri)) { count, name ->
                                viewModelScope.launch(Dispatchers.Main.immediate) {
                                    _uiState.update { it.copy(scanProgressCount = count, scanCurrentFile = name) }
                                }
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "No ROM folders added. Add a folder first.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Scan failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
            val newCount = repository.getGameCount()
            _uiState.update { it.copy(gameCount = newCount, isRescanning = false, scanCurrentFile = null) }
        }
    }

    /** Scrape only games still PENDING. Runs in app scope — continues when you leave Settings. */
    fun rescrapeAll() {
        scrapingController.startScraping()
    }

    /** Reset all to PENDING then scrape. Runs in app scope — continues when you leave Settings. */
    fun resetAndRescrapeAll() {
        scrapingController.resetAndStartScraping()
    }

    /** Stop the current scrape. */
    fun stopScraping() {
        scrapingController.stopScraping()
    }

    fun clearLibrary() {
        viewModelScope.launch {
            repository.deleteAllGames()
            preferencesManager.setSetupComplete(false)
            _uiState.update { it.copy(gameCount = 0, platformEmulators = emptyMap()) }
        }
    }

    fun setEmulatorPreference(platformTag: String, packageName: String) {
        viewModelScope.launch {
            preferencesManager.setEmulatorPreference(platformTag, packageName)
            loadSettings()
        }
    }

    fun clearEmulatorPreference(platformTag: String) {
        viewModelScope.launch {
            preferencesManager.setEmulatorPreference(platformTag, "")
            loadSettings()
        }
    }

    fun updateTheGamesDbApiKey(value: String) {
        _uiState.update { it.copy(theGamesDbApiKey = value, theGamesDbSaved = false) }
    }

    fun saveTheGamesDbApiKey() {
        viewModelScope.launch {
            preferencesManager.setTheGamesDbApiKey(_uiState.value.theGamesDbApiKey)
            _uiState.update { it.copy(theGamesDbSaved = true) }
        }
    }

    fun updateRawgApiKey(value: String) {
        _uiState.update { it.copy(rawgApiKey = value, rawgSaved = false) }
    }

    fun saveRawgApiKey() {
        viewModelScope.launch {
            preferencesManager.setRawgApiKey(_uiState.value.rawgApiKey)
            _uiState.update { it.copy(rawgSaved = true) }
        }
    }

    fun updateMobyGamesApiKey(value: String) {
        _uiState.update { it.copy(mobyGamesApiKey = value, mobyGamesSaved = false) }
    }

    fun saveMobyGamesApiKey() {
        viewModelScope.launch {
            preferencesManager.setMobyGamesApiKey(_uiState.value.mobyGamesApiKey)
            _uiState.update { it.copy(mobyGamesSaved = true) }
        }
    }

    fun setDefaultScraper(scraper: String) {
        viewModelScope.launch {
            preferencesManager.setDefaultScraper(scraper)
            _uiState.update { it.copy(defaultScraper = scraper) }
        }
    }

    fun updateSsDevId(value: String) { _uiState.update { it.copy(ssDevId = value, ssSaved = false) } }
    fun updateSsDevPass(value: String) { _uiState.update { it.copy(ssDevPass = value, ssSaved = false) } }
    fun updateSsUser(value: String) { _uiState.update { it.copy(ssUser = value, ssSaved = false) } }
    fun updateSsPass(value: String) { _uiState.update { it.copy(ssPass = value, ssSaved = false) } }

    fun saveSsCredentials() {
        viewModelScope.launch {
            val s = _uiState.value
            preferencesManager.setSsDevId(s.ssDevId)
            preferencesManager.setSsDevPass(s.ssDevPass)
            preferencesManager.setSsUser(s.ssUser)
            preferencesManager.setSsPass(s.ssPass)
            _uiState.update { it.copy(ssSaved = true) }
        }
    }
    fun dismissScrapePrompt() {
        _uiState.update { it.copy(showScrapePrompt = false) }
    }

    fun startScrapeFromPrompt() {
        _uiState.update { it.copy(showScrapePrompt = false, isScraping = true) }
        viewModelScope.launch {
            scrapingController.startScraping()
        }
    }
}
